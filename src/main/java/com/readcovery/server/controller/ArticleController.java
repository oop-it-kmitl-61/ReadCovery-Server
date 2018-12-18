package com.readcovery.server.controller;

import com.readcovery.server.model.Article;
import com.readcovery.server.model.History;
import com.readcovery.server.model.User;
import com.readcovery.server.model.UserToken;
import com.readcovery.server.repository.*;
import com.readcovery.server.response.ArticleListResponse;
import com.readcovery.server.utils.ArticleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    UserTokenRepository userTokenRepository;

    @Autowired
    HistoryRepository historyRepository;

    static final Map<String, Integer> categoryMap = new HashMap<String, Integer>();
    static{
        categoryMap.put("political", 1);
    }

    @GetMapping("/all")
    public ArticleListResponse getArticle(
            @RequestParam(value="token") String token ){

        List<UserToken> userTokens = userTokenRepository.findByToken(token);
        List<History> historys = historyRepository.findByUserId(userTokens.get(0).getUserId());
        List<Article> articles = articleRepository.findAll();

        List<Article> response = ArticleUtils.getArticleNotInHistory(articles, historys);
        return new ArticleListResponse(response);
    }

    @GetMapping("/category/{category}")
    public ArticleListResponse getArticleByCategory(
            @PathVariable(value="category") String category,
            @RequestParam(value="token") String token ){

        List<UserToken> userTokens = userTokenRepository.findByToken(token);
        List<History> historys = historyRepository.findByUserId(userTokens.get(0).getUserId());
        String[] categorys = category.split(",");
        List<Article> results = new ArrayList<>();

        for(String categorysItem: categorys){
            long categoryId = -1;
            if(categoryMap.containsKey(categorysItem)) {
                categoryId = categoryMap.get(categorysItem.toLowerCase());

                List<Article> articles = articleRepository.findByCategory(categoryId);
                results.addAll(articles);
            }
        }

        List<Article> response = ArticleUtils.getArticleNotInHistory(results, historys);

        return new ArticleListResponse(response);
    }

}
