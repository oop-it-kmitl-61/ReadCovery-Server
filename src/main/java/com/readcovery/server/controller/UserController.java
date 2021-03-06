package com.readcovery.server.controller;

import com.readcovery.server.exception.ArticleNotFoundException;
import com.readcovery.server.exception.ResourceNotFoundException;
import com.readcovery.server.exception.UserAuthenticationException;
import com.readcovery.server.model.*;
import com.readcovery.server.repository.*;
import com.readcovery.server.response.ArticleListResponse;
import com.readcovery.server.response.ReadArticleResponse;
import com.readcovery.server.utils.PasswordUtils;
import com.readcovery.server.utils.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserTokenRepository userTokenRepository;

    @Autowired
    HistoryRepository historyRepository;

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    SaveArticleRepository saveArticleRepository;

    @GetMapping("{id}")
    public User getUserById(@PathVariable(value="id") Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public User createUser(@RequestParam Map<String, String> user){
        String name = user.get("name");
        String password = user.get("password");
        String email = user.get("email");
        String interestedCategory = user.get("interested_category");

        User newUser = new User(name, password, email, interestedCategory);
        return userRepository.save(newUser);
    }

    @PostMapping(path = "/auth", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public UserToken auth(@RequestParam Map<String, String> user){
        String email = user.get("email");
        String password = user.get("password");

        List<User> results = userRepository.findByEmail(email);
        if(results.size() == 0){
            throw new UserAuthenticationException();
        }

        User userdata = results.get(0);
        if(!PasswordUtils.match(password, userdata.getPassword())){
            throw new UserAuthenticationException();
        }

        RandomString genToken = new RandomString();

        UserToken newToken = new UserToken();
        newToken.setToken(genToken.nextString());
        newToken.setUserId(results.get(0).getId());

        return userTokenRepository.save(newToken);
    }

    @GetMapping("/read/{id}")
    public ReadArticleResponse readedArticle(
            @PathVariable long id,
            @RequestParam Map<String, String> article ){

        long articleId = id;
        if(!articleRepository.existsById(articleId)){
            throw new ArticleNotFoundException(articleId);
        }

        List<UserToken> userToken = userTokenRepository.findByToken(article.get("token"));

        Article articleData = articleRepository.findById(articleId).orElseThrow(
                () -> new ResourceNotFoundException("User", "id", id)
        );

        History history = new History(articleId, userToken.get(0).getUserId());
        historyRepository.save(history);

        ReadArticleResponse response = new ReadArticleResponse(articleData, true);

        return response;
    }

    @GetMapping("/read/save/{id}")
    public SaveArticle saveArticle(
            @PathVariable long id,
            @RequestParam(value="token") String token ){

        long articleId = id;
        if(!articleRepository.existsById(articleId)){
            throw new ArticleNotFoundException(articleId);
        }

        List<UserToken> userToken = userTokenRepository.findByToken(token);

        SaveArticle saveArticle = new SaveArticle(articleId, userToken.get(0).getUserId());
        return saveArticleRepository.save(saveArticle);
    }

    @GetMapping("/history")
    public ArticleListResponse getHistory(
            @RequestParam(value="token") String token ){

        List<UserToken> userToken = userTokenRepository.findByToken(token);
        List<History> histories = historyRepository.findByUserId(userToken.get(0).getUserId());
        List<Article> results = new ArrayList<>();

        for(History history: histories){
            long articleId = history.getArticleId();
            results.add(
                    articleRepository.findById(articleId).orElseThrow(
                            () -> new ArticleNotFoundException(articleId)
                    )
            );
        }

        return new ArticleListResponse(results);
    }

    @GetMapping("/getsave")
    public ArticleListResponse getSavedArticles(
            @RequestParam(value="token") String token ){

        List<UserToken> userToken = userTokenRepository.findByToken(token);
        List<SaveArticle> savedArticles = saveArticleRepository.findByUserId(userToken.get(0).getUserId());
        List<Article> results = new ArrayList<>();

        for(SaveArticle saveArticle: savedArticles){
            long articleId = saveArticle.getArticleId();
            results.add(
                    articleRepository.findById(articleId).orElseThrow(
                            () -> new ArticleNotFoundException(articleId)
                    )
            );
        }

        return new ArticleListResponse(results);
    }

    @GetMapping("/category")
    public List<String> getUserCategory(
            @RequestParam(value="token") String token ){

        List<UserToken> userToken = userTokenRepository.findByToken(token);
        User user = userRepository.findById(userToken.get(0).getUserId()).orElseThrow(
                () -> new UserAuthenticationException()
        );

        List<String> categorys = Arrays.asList(user.getInterestedCategory().split(","));

        return categorys;
    }

    @PostMapping("/category/update")
    public User updateCategory(
            @RequestParam(value="token") String token,
            @RequestParam Map<String, String> body ){

        List<UserToken> userToken = userTokenRepository.findByToken(token);
        User user = userRepository.findById(userToken.get(0).getUserId()).orElseThrow(
                () -> new UserAuthenticationException()
        );

        user.setInterestedCategory(body.get("category"));
        User saveUser = userRepository.save(user);

        return saveUser;
    }
}
