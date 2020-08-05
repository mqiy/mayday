package com.songhaozhi.mayday.model.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag {
	private Integer tagId;

	private String tagName;

	private String tagUrl;

}