package com.songhaozhi.mayday.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Article {

	private Integer id;

	private Integer userId;

	private String articleContent;

	private String articleContentMd;

	private Date articleNewstime;

	private Integer articleStatus;

	private String articleSummary;

	private String articleThumbnail;

	private String articleTitle;

	private Integer articleType;

	private String articlePost;

	private Integer articleComment;

	private Date articleUpdatetime;

	private String articleUrl;

	private Long articleViews;

}