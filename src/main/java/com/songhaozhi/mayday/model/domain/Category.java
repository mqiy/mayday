package com.songhaozhi.mayday.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category implements Serializable {


	private static final long serialVersionUID = 1660254568965065323L;

	private Integer categoryId;

	private String categoryName;

	private String categoryUrl;

	private String categoryDescribe;
}