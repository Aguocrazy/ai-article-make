package com.aiarticle.model.state;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文章生成链路的状态容器（工作台）。
 * <p>
 * 五个智能体串行执行，后一个要读前一个的产出。每个智能体做完把结果放进本对象，
 * 下一个再从这里取材料继续写。
 * <ul>
 *   <li>taskId、topic：任务基础信息</li>
 *   <li>title：智能体1 标题（主标题 + 副标题）</li>
 *   <li>outline：智能体2 大纲</li>
 *   <li>content：智能体3 正文</li>
 *   <li>imageRequirements：智能体4 配图需求</li>
 *   <li>images：智能体5 配图结果</li>
 *   <li>fullContent：最终合成的完整图文</li>
 * </ul>
 */
@Data
public class ArticleState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 标题结果（智能体1 输出）
     */
    private TitleResult title;

    /**
     * 大纲结果（智能体2 输出）
     */
    private OutlineResult outline;

    /**
     * 正文内容（智能体3 输出）
     */
    private String content;

    /**
     * 配图需求列表（智能体4 输出）
     */
    private List<ImageRequirement> imageRequirements;

    /**
     * 封面图 URL（单独存储；images 中 position=1 的也是封面图）
     */
    private String coverImage;

    /**
     * 配图结果列表（智能体5 输出）
     */
    private List<ImageResult> images;

    /**
     * 完整图文内容（合成后）
     */
    private String fullContent;

    /**
     * 标题结果
     */
    @Data
    public static class TitleResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主标题
         */
        private String mainTitle;

        /**
         * 副标题
         */
        private String subTitle;
    }

    /**
     * 大纲结果
     */
    @Data
    public static class OutlineResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 章节列表
         */
        private List<OutlineSection> sections;
    }

    /**
     * 大纲章节
     */
    @Data
    public static class OutlineSection implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 章节序号
         */
        private Integer section;

        /**
         * 章节标题
         */
        private String title;

        /**
         * 要点
         */
        private List<String> points;
    }

    /**
     * 配图需求
     */
    @Data
    public static class ImageRequirement implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 位置（1 为封面）
         */
        private Integer position;

        /**
         * 配图类型
         */
        private String type;

        /**
         * 对应章节标题
         */
        private String sectionTitle;

        /**
         * 关键词
         */
        private String keywords;
    }

    /**
     * 配图结果
     */
    @Data
    public static class ImageResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 位置（1 为封面）
         */
        private Integer position;

        /**
         * 图片 URL
         */
        private String url;

        /**
         * 获取方式
         */
        private String method;

        /**
         * 关键词
         */
        private String keywords;

        /**
         * 对应章节标题
         */
        private String sectionTitle;

        /**
         * 说明
         */
        private String description;
    }
}
