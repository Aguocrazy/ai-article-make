-- 文章创作设定：类型、语气、篇幅、读者、补充要求
alter table article
    add column articleType varchar(64) null comment '文章类型' after topic,
    add column writingTone varchar(64) null comment '写作语气' after articleType,
    add column wordCount int null comment '目标字数' after writingTone,
    add column audience varchar(200) null comment '目标读者' after wordCount,
    add column extraRequirement text null comment '补充写作要求' after audience;
