package com.convo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.convo.datamodel.Tweet;
import com.convo.datamodel.TweetLike;
import com.convo.datamodel.User;
import com.convo.kafka.TweetsProducer;
import com.convo.repository.TweetLikeRepository;
import com.convo.repository.TweetsRepository;
import com.convo.restmodel.LikeTweetRequest;
import com.convo.restmodel.ListTweetsRequest;
import com.convo.restmodel.ListTweetsResponse;
import com.convo.restmodel.TweetDeleteRequest;
import com.convo.restmodel.TweetEditRequest;
import com.convo.restmodel.TweetSaveRequest;
import com.convo.util.GsonUtils;
import com.convo.util.SystemContext;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/tweet/v1")
@Slf4j
public class TweetController {

	@Autowired
	private TweetsProducer tweetsProducer;

	@Autowired
	private TweetsRepository tweetsRepo;

	@Autowired
	private TweetLikeRepository tweetLikeRepo;

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	protected void saveTweet(@RequestBody TweetSaveRequest tweetSaveRequest) {
		log.info("Tweet save request {}", GsonUtils.getGson().toJson(tweetSaveRequest));
		User loggedInUser = SystemContext.getLoggedInUser();
		Tweet tweet = Tweet.builder().userId(loggedInUser.getUserId()).tweet(tweetSaveRequest.getTweet())
				.createdOn(LocalDateTime.now()).processedOn(LocalDateTime.now()).build();
		tweetsProducer.sendTweet(tweet);
	}

	@RequestMapping(value = "/edit", method = RequestMethod.PATCH)
	protected void editTweet(@RequestBody TweetEditRequest tweetEditRequest) {
		log.info("Tweet edit request {}", GsonUtils.getGson().toJson(tweetEditRequest));
		User loggedInUser = SystemContext.getLoggedInUser();
		Tweet tweet = Tweet.builder().id(tweetEditRequest.getTweetId()).tweet(tweetEditRequest.getTweet())
				.userId(loggedInUser.getUserId()).build();
		tweetsProducer.sendTweet(tweet);
	}

	@RequestMapping(value = "/list", method = RequestMethod.POST)
	protected ResponseEntity<ListTweetsResponse> listTweets(@RequestBody ListTweetsRequest listTweetsRequest) {
		Pageable pageable = PageRequest.of(listTweetsRequest.getOffset(), 100, Sort.by("createdOn").descending());
		Page<Tweet> tweetsPage = tweetsRepo.findAll(pageable);
		List<Tweet> tweets = tweetsPage.stream().collect(Collectors.toList());
		return new ResponseEntity<>(
				ListTweetsResponse.builder().offset(listTweetsRequest.getOffset()).tweets(tweets).build(),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	protected void deleteTweet(@RequestBody TweetDeleteRequest tweetDeleteRequest) {
		User loggedInUser = SystemContext.getLoggedInUser();
		Long tweetId = tweetDeleteRequest.getTweetId();
		Tweet tweet = tweetsRepo.findById(tweetId).orElse(null);
		if (tweet != null && tweet.getUserId().equals(loggedInUser.getUserId())) {
			tweet.setIsDeleted(true);
			tweetsRepo.save(tweet);
		} else {
			throw new RuntimeException("Tweet not found");
		}
	}

	@RequestMapping(value = "/show/{id}", method = RequestMethod.GET)
	protected Tweet showTweet(@PathVariable Long id) {
		if (id == null) {
			throw new RuntimeException("Id to dede bhai");
		}
		User loggedInUser = SystemContext.getLoggedInUser();
		Tweet tweet = tweetsRepo.findById(id).orElse(null);
		if (tweet != null && tweet.getUserId().equals(loggedInUser.getUserId())) {
			return tweet;
		} else {
			throw new RuntimeException("Tweet not found");
		}
	}

	@RequestMapping(value = "/like", method = RequestMethod.POST)
	protected void likeTweet(@RequestBody LikeTweetRequest request) {
		Long tweetId = request.getTweetId();
		if (tweetId == null) {
			throw new RuntimeException("Tweet id is required");
		}
		User loggedInUser = SystemContext.getLoggedInUser();
		TweetLike tweetLike = tweetLikeRepo.findByTweetIdAndUserId(tweetId, loggedInUser.getUserId());
		if (tweetLike == null) {
			tweetLike = TweetLike.builder().tweetId(tweetId).userId(loggedInUser.getUserId())
					.createdOn(LocalDateTime.now()).build();
			tweetLikeRepo.save(tweetLike);
		} else {
			throw new RuntimeException("Tweet already liked");
		}
	}

}
