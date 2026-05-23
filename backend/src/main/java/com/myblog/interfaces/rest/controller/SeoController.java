package com.myblog.interfaces.rest.controller;

import com.myblog.domain.model.aggregate.Article;
import com.myblog.domain.model.aggregate.Column;
import com.myblog.domain.model.aggregate.Topic;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.TopicRepository;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SEO 相关接口控制器。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
public class SeoController {

    private static final DateTimeFormatter SITEMAP_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+08:00'");
    private static final DateTimeFormatter RSS_DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;
    private static final int RSS_LIMIT = 20;
    private static final String SEO_CACHE_NAME = "seoCache";

    private final ArticleRepository articleRepository;
    private final ColumnRepository columnRepository;
    private final TopicRepository topicRepository;
    private final RMapCache<String, String> seoCache;

    public SeoController(ArticleRepository articleRepository,
                         ColumnRepository columnRepository,
                         TopicRepository topicRepository,
                         RedissonClient redissonClient) {
        this.articleRepository = articleRepository;
        this.columnRepository = columnRepository;
        this.topicRepository = topicRepository;
        this.seoCache = redissonClient.getMapCache(SEO_CACHE_NAME);
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain; charset=utf-8")
    public String robotsTxt() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("User-agent: *\n");
        sb.append("Allow: /\n");
        sb.append("Disallow: /api/admin/\n");
        sb.append("Disallow: /api/dashboard/\n");
        sb.append("Disallow: /api/settings/\n");
        sb.append("Disallow: /api/notifications/\n");
        sb.append("Disallow: /api/uploads/\n");
        sb.append("\n");
        sb.append("Sitemap: https://myblog.example.com/sitemap.xml\n");
        return sb.toString();
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml; charset=utf-8")
    public String sitemapXml() {
        String cached = seoCache.get("sitemap");
        if (cached != null) {
            return cached;
        }
        String xml = buildSitemap();
        seoCache.put("sitemap", xml, CACHE_TTL_MS, TimeUnit.MILLISECONDS);
        return xml;
    }

    @GetMapping(value = "/rss.xml", produces = "application/xml; charset=utf-8")
    public String rssXml() {
        String cached = seoCache.get("rss");
        if (cached != null) {
            return cached;
        }
        String xml = buildRss();
        seoCache.put("rss", xml, CACHE_TTL_MS, TimeUnit.MILLISECONDS);
        return xml;
    }

    private String buildSitemap() {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        List<Article> articles = articleRepository.findPublishedWithLimit(null, null, null, null, Integer.MAX_VALUE, 0);
        for (Article article : articles) {
            String slug = article.getSlug();
            String url = slug != null && !slug.isEmpty()
                ? "/articles/" + article.getId().getValue() + "-" + slug
                : "/articles/" + article.getId().getValue();
            appendUrl(xml, url, article.getUpdatedAt());
        }

        List<Column> columns = columnRepository.findPublished(1, Integer.MAX_VALUE);
        for (Column column : columns) {
            appendUrl(xml, "/columns/" + column.getId().getValue(), column.getUpdatedAt());
        }

        List<Topic> topics = topicRepository.findPublished(1, Integer.MAX_VALUE);
        for (Topic topic : topics) {
            appendUrl(xml, "/topics/" + topic.getId().getValue(), topic.getUpdatedAt());
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(StringBuilder xml, String url, LocalDateTime lastMod) {
        xml.append("  <url>\n");
        xml.append("    <loc>https://myblog.example.com").append(url).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(lastMod.format(SITEMAP_DATE_FMT)).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    private String buildRss() {
        List<Article> articles = articleRepository.findPublishedWithLimit(null, null, null, "latest", RSS_LIMIT, 0);

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>my-blog</title>\n");
        xml.append("    <link>https://myblog.example.com</link>\n");
        xml.append("    <description>一个面向技术创作者和读者的博客社区平台</description>\n");
        xml.append("    <language>zh-CN</language>\n");
        xml.append("    <atom:link href=\"https://myblog.example.com/rss.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (Article article : articles) {
            String slug = article.getSlug();
            String url = slug != null && !slug.isEmpty()
                ? "/articles/" + article.getId().getValue() + "-" + slug
                : "/articles/" + article.getId().getValue();

            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(article.getTitle())).append("</title>\n");
            xml.append("      <link>https://myblog.example.com").append(url).append("</link>\n");
            xml.append("      <description>").append(escapeXml(article.getSummary())).append("</description>\n");
            if (article.getPublishedAt() != null) {
                xml.append("      <pubDate>").append(article.getPublishedAt().format(RSS_DATE_FMT)).append("</pubDate>\n");
            }
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
