package com.songhaozhi.mayday.web.controller.front;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.github.pagehelper.PageInfo;
import com.songhaozhi.mayday.model.domain.Article;
import com.songhaozhi.mayday.model.domain.ArticleCustom;
import com.songhaozhi.mayday.model.domain.Category;
import com.songhaozhi.mayday.model.domain.Link;
import com.songhaozhi.mayday.model.domain.Tag;
import com.songhaozhi.mayday.model.dto.ArchiveBo;
import com.songhaozhi.mayday.model.dto.MaydayConst;
import com.songhaozhi.mayday.model.enums.ArticleStatus;
import com.songhaozhi.mayday.model.enums.PageNumber;
import com.songhaozhi.mayday.model.enums.PostType;
import com.songhaozhi.mayday.service.ArticleService;
import com.songhaozhi.mayday.service.CategoryService;
import com.songhaozhi.mayday.service.LinksService;
import com.songhaozhi.mayday.service.TagService;
import com.songhaozhi.mayday.util.MaydayUtil;
import com.songhaozhi.mayday.web.controller.admin.BaseController;
import com.sun.syndication.io.FeedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : 宋浩志
 * @createDate : 2018年12月2日
 * 
 */
@Controller
public class IndexController extends BaseController {
	@Autowired
	private ArticleService articleService;
	@Autowired
	private LinksService linksService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private TagService tagService;

	/**
	 * 请求首页
	 *
	 * @param model
	 * @return 模板路径
	 */
	@GetMapping(value = { "/", "index" })
	public String index(Model model) {
		// 调用方法渲染首页
		return this.index(model, 1);
	}

	/**
	 * 首页分页
	 *
	 * @return 模板路径/themes/{theme}/index
	 */
	@GetMapping(value = "page/{page}")
	public String index(Model model, @PathVariable(value = "page") Integer page) {
		page = page < 0 || page > MaydayConst.MAX_PAGE ? 1 : page;
		// 默认显示条数
		Integer limit = StrUtil.isEmpty(MaydayConst.OPTIONS.get("index_article")) ? PageNumber.POST_INDEX_lIMIT.getLimit()
				: Integer.parseInt(MaydayConst.OPTIONS.get("index_article"));
		ArticleCustom articleCustom = new ArticleCustom();
		articleCustom.setArticleStatus(ArticleStatus.PUBLISH.getStatus());
		articleCustom.setArticlePost(PostType.POST_TYPE_POST.getValue());
		PageInfo<ArticleCustom> pageInfo = articleService.findPageArticle(page, limit, articleCustom);
		model.addAttribute("articles", pageInfo);
		return this.render("index");
	}

	/**
	 * 归档 BY LHY
	 * 
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "archives")
	public String archives(Model model) throws Exception {
		List<ArchiveBo> archiveBos = articleService.archives();
		model.addAttribute("articleList", archiveBos);
		return this.render("archives");
	}

	/**
	 * 文章页
	 * 
	 * @param model
	 * @param articleUrl
	 *            文章url
	 * @return
	 */
	@GetMapping(value = { "post/{articleUrl}", "post/{articleUrl}.html" })
	public String post(Model model, @PathVariable(value = "articleUrl") String articleUrl, HttpServletRequest request) {
		ArticleCustom articleCustom = articleService.findByArticleUrl(articleUrl);
		if (articleCustom == null) {
			return this.render_404();
		}
		if (!checkRepeatIp(request, articleCustom.getId())) {
			updateArticleViews(articleCustom.getId(), articleCustom.getArticleViews());
		}
		model.addAttribute("article", articleCustom);
		
		

		List<ArticleCustom>  allRelationList = new ArrayList<>();


		List<ArticleCustom>  relations = new ArrayList<>();

		Integer limit = PageNumber.POST_INDEX_lIMIT.getLimit();

		if(StringUtils.isNotEmpty(articleCustom.getCategorys())){
			Category category =  categoryService.findByCategoryId(Integer.parseInt(articleCustom.getCategorys().split(",")[0]));
			PageInfo<ArticleCustom> info = articleService.findArticleByCategory(1, limit, category, ArticleStatus.PUBLISH.getStatus());
			allRelationList.addAll(info.getList());
		}

		if(CollectionUtils.isEmpty(allRelationList)){
			if(StringUtils.isNotEmpty(articleCustom.getTags())){
				Tag tag =  tagService.findByTagId(Integer.parseInt(articleCustom.getTags().split(",")[0]));
				PageInfo<ArticleCustom> info = articleService.findArticleByTag(1, limit, tag, ArticleStatus.PUBLISH.getStatus());
				allRelationList.addAll(info.getList());
			}
		}


		//取前各后一条
		if (!CollectionUtils.isEmpty(allRelationList)) {
			int index =-1;

			int before=-1;
			int after = -1;

			for (int i = 0; i < allRelationList.size(); i++) {
				if(Objects.equals(articleCustom.getId(),allRelationList.get(i).getId())){
					index = i;
					break;
				}
			}

			//理论上是能找到的
			if (allRelationList.size() > 2) {
				if (index == 0) {
					after = 1;
				} else if (index == allRelationList.size() - 1) {
					before = index - 1;
				} else {
					before = index - 1;

					after = index + 1;
				}
			} else if (allRelationList.size() == 2) {
				if (index == 0) {
					after = 1;
				} else {
					before = 0;
				}
			}

			if(before>-1){
				ArticleCustom beforeArticle= allRelationList.get(before);
				beforeArticle.setArticleSummary("上一条");
				relations.add(beforeArticle);
			}

			if(after>-1){
				ArticleCustom afterArticle= allRelationList.get(after);
				afterArticle.setArticleSummary("下一条");
				relations.add(afterArticle);
			}
		}


		model.addAttribute("relations", relations);
		
		return this.render("post");
	}

	/**
	 * 检测同一IP十分钟以内重复访问同一篇文章
	 * 
	 * @param request
	 * @param id
	 *            文章id
	 * @return
	 */
	public boolean checkRepeatIp(HttpServletRequest request, int id) {

		String value = ServletUtil.getClientIP(request) + ":" + id;
		Integer count = cache.hget("hits:frequency", value);
		if (count != null && count > 0) {
			return true;
		}
		cache.hset("hits:frequency", value, 1, MaydayConst.IP_REPEAT_TIME);
		return false;
	}

	/**
	 * 修改文章点击率
	 * 
	 * @param id
	 * @param views
	 */
	public void updateArticleViews(Integer id, Long views) {
		if (views == null) {
			views = 0L;
		}
		// 获取缓存数据
		Long cacheViews = cache.hget("article" + id, "cacheViews");
		// 如果缓存数据为null赋值1，反之加1
		cacheViews = cacheViews == null ? 1 : cacheViews + 1;
		// 如果缓存只大于等于设置的次数则修改到数据库
		if (cacheViews >= MaydayConst.CLICK_EXCEED) {
			Article article = new Article();
			article.setId(id);
			article.setArticleViews(views + cacheViews);
			articleService.updateArticleViews(article);
			cache.hset("article" + id, "cacheViews", null);
		} else {
			cache.hset("article" + id, "cacheViews", cacheViews);
		}
	}

	/**
	 * 友链
	 * 
	 * @param model
	 * @return
	 */
	@GetMapping(value = "links")
	public String links(Model model) {
		List<Link> links = linksService.findLinks();
		model.addAttribute("links", links);
		return this.render("links");
	}

	/**
	 * 自建页面
	 * 
	 * @param articleUrl
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/{articleUrl}")
	public String page(@PathVariable String articleUrl, Model model, HttpServletRequest request) {
		ArticleCustom articleCustom = articleService.findByArticleUrl(articleUrl);
		if (articleCustom == null) {
			return this.render_404();
		}
		if (!checkRepeatIp(request, articleCustom.getId())) {
			updateArticleViews(articleCustom.getId(), articleCustom.getArticleViews());
		}
		model.addAttribute("article", articleCustom);
		return this.render("page");
	}

	/**
	 * 分类页
	 *
	 * @return
	 */
	@GetMapping(value = "category/{cateUrl}")
	public String categories(Model model, @PathVariable String cateUrl) {
		return this.categories(model, cateUrl, 1);
	}

	@GetMapping(value = "category/{cateUrl}/{page}")
	public String categories(Model model, @PathVariable String cateUrl, @PathVariable Integer page) {
		// 验证是否存在该路径
		Category category = categoryService.findByCateUrl(cateUrl);
		if (category == null) {
			this.render_404();
		}
		Integer limit = PageNumber.POST_INDEX_lIMIT.getLimit();
		page = page < 0 || page > MaydayConst.MAX_PAGE ? 1 : page;
		PageInfo<ArticleCustom> info = articleService.findArticleByCategory(page, limit, category,
				ArticleStatus.PUBLISH.getStatus());
		model.addAttribute("articles", info);
		model.addAttribute("type", "分类");
		model.addAttribute("url", "category/" + category.getCategoryUrl());
		model.addAttribute("keywords", category.getCategoryName());
		return this.render("page-category");
	}


	/**
	 * 标签列表
	 *
	 * @return
	 */
	@GetMapping(value = "tags")
	public String showTags(Model model) {

		List<Tag> tagList = tagService.findTags();
		StringBuffer nameBuffer = new StringBuffer();
		StringBuffer urlBuffer = new StringBuffer();
		for(Tag tag:tagList){
			nameBuffer.append(tag.getTagName());
			nameBuffer.append(",");
			urlBuffer.append(tag.getTagUrl());
			urlBuffer.append(",");
		}
		model.addAttribute("tagsUrl", urlBuffer.toString());
		model.addAttribute("tagsName", nameBuffer.toString());
		model.addAttribute("keywords","标签列表");
		return this.render("tags");
	}
	/**
	 * 分类列表
	 *
	 * @return
	 */
	@GetMapping(value = "categories")
	public String showCategories(Model model) {

		List<Category> tagList = categoryService.findCategory();
		StringBuffer nameBuffer = new StringBuffer();
		StringBuffer urlBuffer = new StringBuffer();
		for(Category category:tagList){
			nameBuffer.append(category.getCategoryName());
			nameBuffer.append(",");
			urlBuffer.append(category.getCategoryUrl());
			urlBuffer.append(",");
		}
		model.addAttribute("categoriesUrl", urlBuffer.toString());
		model.addAttribute("categoriesName", nameBuffer.toString());
		model.addAttribute("keywords","分类列表");
		return this.render("categories");
	}


	/**
	 * 标签页
	 *
	 * @return
	 */
	@GetMapping(value = "tags/{tagUrl}")
	public String tags(Model model, @PathVariable String tagUrl) {
		return this.tags(model, tagUrl, 1);
	}

	@GetMapping(value = "tags/{tagUrl}/{page}")
	public String tags(Model model, @PathVariable String tagUrl, @PathVariable Integer page) {
		// 验证是否存在该路径
		Tag tag = tagService.findByTagUrl(tagUrl);
		if (tag == null) {
			this.render_404();
		}
		Integer limit = PageNumber.POST_INDEX_lIMIT.getLimit();
		page = page < 0 || page > MaydayConst.MAX_PAGE ? 1 : page;
		PageInfo<ArticleCustom> info = articleService.findArticleByTag(page, limit, tag,
				ArticleStatus.PUBLISH.getStatus());
		model.addAttribute("articles", info);
		model.addAttribute("type", "标签");
		model.addAttribute("url", "tags/" + tag.getTagUrl());
		model.addAttribute("keywords", tag.getTagName());
		return this.render("page-category");
	}

	/**
	 * 标题搜索
	 *
	 * @return
	 */
	@GetMapping(value = "search/{keywords}")
	public String search(Model model, @PathVariable String keywords) {
		return this.search(model, keywords, 1);
	}

	@GetMapping(value = "search/{keywords}/{page}")
	public String search(Model model, @PathVariable String keywords, @PathVariable Integer page) {
		Integer limit = PageNumber.POST_INDEX_lIMIT.getLimit();
		page = page < 0 || page > MaydayConst.MAX_PAGE ? 1 : page;
		PageInfo<Article> info = articleService.findArticleByKeywords(keywords, page, limit);
		model.addAttribute("articles", info);
		model.addAttribute("type", "搜索");
		model.addAttribute("keywords", keywords);
		model.addAttribute("url", "search/" + keywords);
		return this.render("page-category");
	}

	/**
	 * 文章rss
	 * 
	 * @return
	 * @throws FeedException 
	 */
	@GetMapping(value = { "feed", "feed.xml", "index.xml", "atom", "atom.xml" }, produces = "application/xml;charset=UTF-8")
	@ResponseBody
	public String rss() throws FeedException {
		String rssNumber = MaydayConst.OPTIONS.get("rss_number");
		if (StrUtil.isBlank(rssNumber)) {
			rssNumber = "20";
		}
		ArticleCustom articleCustom = new ArticleCustom();
		articleCustom.setArticleStatus(ArticleStatus.PUBLISH.getStatus());
		articleCustom.setArticlePost(PostType.POST_TYPE_POST.getValue());
		PageInfo<ArticleCustom> pageInfo = articleService.findPageArticle(0, Integer.parseInt(rssNumber), articleCustom);
		List<ArticleCustom> articles=pageInfo.getList();
		return MaydayUtil.buildRss(articles);
	}

}
