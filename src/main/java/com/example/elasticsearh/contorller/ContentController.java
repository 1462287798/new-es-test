package com.example.elasticsearh.contorller;

import com.example.elasticsearh.service.ContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class ContentController {
    @Autowired
    private ContentService contentService;

    //http://localhost:8080/search/java/1/30
    @GetMapping("/search/{keyword}/{pageNo}/{pageSize}")
    @CrossOrigin
    public List<Map<String, Object>> search(
            @PathVariable("keyword") String keyword,
            @PathVariable("pageNo") int pageNo,
            @PathVariable("pageSize") int pageSize) throws IOException {
        return contentService.searchPageHighlightBuilder(keyword, pageNo, pageSize);
    }
}