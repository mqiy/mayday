package com.songhaozhi.mayday.util;


import cn.hutool.core.collection.CollectionUtil;
import com.songhaozhi.mayday.mapper.generator.*;
import com.songhaozhi.mayday.model.domain.*;
import com.youbenzi.mdtool.tool.MDTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class ConvertBz {

    @Autowired
    private ArticleMapper articleMapper;


    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;



    public void convert() {
        String path = "E:\\blog\\source\\_posts\\md\\problem";
        File file = new File(path);
        deal(file);
    }


    private void deal(File f) {
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                deal(file);
            }
        } else {
            doSome(f);
        }
    }


    private void doSome(File f) {


        List<String> context = getContext(f);
        if (context.get(0).equals("---")) {

        } else {
            log.warn("[不符合格式,{}]", f.getPath());
            return;
        }


        if (context.get(1).startsWith("title: ")) {

        } else {
            log.warn("[不符合格式,title,{}]", f.getPath());
            return;
        }

        String title = context.get(1).replace("title: ", "");


        if (context.get(2).startsWith("date: ")) {
        } else {
            log.warn("[不符合格式,date,{}]", f.getPath());
            return;
        }
        String postStr = context.get(2).replace("date: ", "");
        Date postDate = getDate(postStr);


        String[] tags = null;
        if (context.get(3).startsWith("tags: ")) {
            String tagStr = context.get(3).replace("tags: ", "");
            tags = tagStr.substring(1, tagStr.length() - 1).split(",");
        }

        if (context.get(4).startsWith("tags: ")) {
            String tagStr = context.get(4).replace("tags: ", "");
            tags = tagStr.substring(1, tagStr.length() - 1).split(",");
        }

        String[] categories = null;
        if (context.get(3).startsWith("category: ")) {
            String tagStr = context.get(3).replace("category: ", "");
            categories = tagStr.substring(1, tagStr.length() - 1).split(",");
        }
        if (context.get(4).startsWith("category: ")) {
            String tagStr = context.get(4).replace("category: ", "");
            categories = tagStr.substring(1, tagStr.length() - 1).split(",");
        }

        if (context.get(3).startsWith("categories: ")) {
            String tagStr = context.get(3).replace("categories: ", "");
            categories = tagStr.substring(1, tagStr.length() - 1).split(",");
        }
        if (context.get(4).startsWith("categories: ")) {
            String tagStr = context.get(4).replace("categories: ", "");
            categories = tagStr.substring(1, tagStr.length() - 1).split(",");
        }

        int index = 0;
        if (context.get(4).equals("---")) {
            index = 5;
        }
        if (context.get(5).equals("---")) {
            index = 6;
        }

        if (index == 0) {
            log.warn("[正文无法确定位置,{}]", f.getPath());
            return;
        }

        while (context.get(index).equals("")) {
            index++;
        }

        System.out.println(context.size());

        StringBuffer buffer = new StringBuffer();

        for (int i = index; i < context.size(); i++) {
            buffer.append(context.get(i));
            buffer.append("\r\n");
        }

        Article article = Article.builder()
                .articleContentMd(buffer.toString())
                .articleContent(MDTool.markdown2Html(buffer.toString()))
                .articleNewstime(postDate)
                .articleTitle(title)
                .articleSummary(f.getName())
                .articlePost("post")
                .articleType(0)
                .articleThumbnail("/static/img/rand/2.jpg")
                .articleUrl(String.valueOf(System.currentTimeMillis()))
                .articleStatus(0).build();

        articleMapper.insert(article);

        if(tags!=null){

            for(String tag:tags){
                if(tag==null || StringUtils.isEmpty(tag)){
                    continue;
                }
                TagExample tagExample = new TagExample();
                TagExample.Criteria criteria = tagExample.createCriteria();
                criteria.andTagNameEqualTo(tag.trim());

                List<Tag>  tagList= tagMapper.selectByExample(tagExample);
                Tag tagInfo =null;
                if(CollectionUtil.isEmpty(tagList)){
                    tagInfo = Tag.builder()
                            .tagName(tag.trim())
                            .tagUrl(tag.trim().replace(" ","-"))
                            .build();
                    tagMapper.insert(tagInfo);
                }

                tagList= tagMapper.selectByExample(tagExample);

                tagInfo = tagList.get(0);

                ArticleTag articleTag = ArticleTag.builder()
                        .articleId(article.getId())
                        .tagId(tagInfo.getTagId().longValue())
                        .build();
                articleTagMapper.insert(articleTag);
            }
        }

        if(categories!=null){

            for(String category:categories){
                if(category==null || StringUtils.isEmpty(category)){
                    continue;
                }
                CategoryExample categoryExample = new CategoryExample();
                CategoryExample.Criteria criteria = categoryExample.createCriteria();
                criteria.andCategoryNameEqualTo(category.trim());

                List<Category>  categoryList= categoryMapper.selectByExample(categoryExample);
                Category categoryInfo =null;
                if(CollectionUtil.isEmpty(categoryList)){
                    categoryInfo = Category.builder()
                            .categoryName(category.trim())
                            .categoryUrl(category.trim().replace(" ","-"))
                            .build();
                    categoryMapper.insert(categoryInfo);
                }
                categoryList= categoryMapper.selectByExample(categoryExample);
                categoryInfo = categoryList.get(0);

                ArticleCategory articleCategory = ArticleCategory.builder()
                        .articleId(article.getId())
                        .categoryId(categoryInfo.getCategoryId().longValue())
                        .build();
                articleCategoryMapper.insert(articleCategory);
            }
        }
        log.info("done,{}",f.getPath());
    }

    private static Date getDate(String str) {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return format.parse(str);
        } catch (ParseException e) {
        }
        return Calendar.getInstance().getTime();
    }


    private static List<String> getContext(File file) {

        List<String> context = new ArrayList<>();


        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                context.add(line);
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

        return context;
    }
}
