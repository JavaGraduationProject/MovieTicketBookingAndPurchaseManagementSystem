CREATE TABLE `user_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '',
    `password` varchar(255) COMMENT '',
    `nick_name` varchar(255) COMMENT '',
    `sex` tinyint(4) COMMENT '',
    `age` tinyint(4) COMMENT '年龄',
    `birthday` datetime COMMENT '@JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")',
    `phone` null COMMENT '',
    `address` null COMMENT '',
    `email` null COMMENT '',
    `card_id` varchar(255) COMMENT '关联订单id',
    `level` int(11) COMMENT '',
    `account` null COMMENT '',
    `confirm_code` varchar(255) COMMENT '确认码',
    `activation_time` datetime COMMENT '失效时间',
    `is_valid` tinyint(4) COMMENT '是否激活',
    `is_ban` tinyint(4) COMMENT '是否禁用',
    `new_password` null COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='用户实体类表';
CREATE TABLE `type_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '类型名称',
    `description` varchar(255) COMMENT '类型描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='电影分类实体来表';
CREATE TABLE `authority_info` (
    `level` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '',
    `models` null COMMENT '',
    PRIMARY KEY (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `advertiser_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '公告名',
    `content` varchar(255) COMMENT '公告内容',
    `time` datetime COMMENT '@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='公告实体类表';
CREATE TABLE `admin_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '',
    `password` varchar(255) COMMENT '',
    `nick_name` varchar(255) COMMENT '',
    `sex` tinyint(4) COMMENT '',
    `age` tinyint(4) COMMENT '年龄',
    `birthday` datetime COMMENT '@JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")',
    `phone` null COMMENT '',
    `address` null COMMENT '',
    `code` varchar(255) COMMENT '编号',
    `email` null COMMENT '',
    `card_id` varchar(255) COMMENT '身份证',
    `level` int(11) COMMENT '',
    `account` null COMMENT '',
    `confirm_code` varchar(255) COMMENT '',
    `activation_time` datetime COMMENT '',
    `is_valid` tinyint(4) COMMENT '',
    `new_password` null COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `link_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '',
    `url` varchar(255) COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `model` (
    `model_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operation` null COMMENT '',
    PRIMARY KEY (`model_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `cart_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `count` int(11) COMMENT '数量',
    `user_id` bigint(20) COMMENT '所属用户',
    `goods_id` bigint(20) COMMENT '所属商品',
    `create_time` datetime COMMENT '创建时间',
    `level` int(11) COMMENT '权限等级',
    `user_name` null COMMENT '用户名',
    `goods_name` null COMMENT '商品名',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='收藏实体类表';
CREATE TABLE `seat_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `position` varchar(255) COMMENT '',
    `goods_id` bigint(20) COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `order_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `order_id` varchar(255) COMMENT '订单id',
    `total_price` double(20,2) COMMENT '订单总价格',
    `user_id` bigint(20) COMMENT '所属用户',
    `level` int(11) COMMENT '',
    `link_address` varchar(255) COMMENT '联系地址',
    `link_phone` varchar(255) COMMENT '联系电话',
    `link_man` varchar(255) COMMENT '联系人',
    `create_time` datetime COMMENT '创建时间',
    `status` tinyint(4) COMMENT '订单状态',
    `user_info` null COMMENT '关联的用户信息',
    `goods_list` null COMMENT '',
    `goods_id` null COMMENT '电影id',
    `total` null COMMENT '',
    `position` null COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='主键id表';
CREATE TABLE `message_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '发表的影评用户名',
    `content` varchar(255) COMMENT '影评内容',
    `time` datetime COMMENT '发表评价时间',
    `parent_id` bigint(20) COMMENT '父级id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='影评实体类表';
CREATE TABLE `nx_system_file_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `origin_name` varchar(255) COMMENT '',
    `file_name` varchar(255) COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='';
CREATE TABLE `comment_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `content` varchar(255) COMMENT '评价内容',
    `goods_id` bigint(20) COMMENT '所属电影',
    `create_time` datetime COMMENT '创建时间',
    `user_id` bigint(20) COMMENT '评论者（当前用户）id',
    `level` int(11) COMMENT '权限等级',
    `goods_name` null COMMENT '电影名',
    `user_name` null COMMENT '用户昵称',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='评论实体类表';
CREATE TABLE `goods_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` varchar(255) COMMENT '商品名称 （电影名称）',
    `description` varchar(255) COMMENT '商品描述（电影描述）',
    `file_ids` varchar(255) COMMENT '商品图片id（电影图片）',
    `price` double(20,2) COMMENT '商品价格（电影价格）',
    `sales` int(11) COMMENT '商品销量（电影销量）',
    `hot` int(11) COMMENT '商品点赞数',
    `actor` varchar(255) COMMENT '参演电影的演员信息',
    `begin_time` datetime COMMENT '@JsonFormat(pattern = "yyyy:MM:dd HH:mm:ss",timezone = "GMT+8")',
    `time` varchar(255) COMMENT '电影放映时长',
    `type_id` bigint(20) COMMENT '所属类别',
    `user_id` bigint(20) COMMENT '所属用户',
    `level` int(11) COMMENT '',
    `is_show` tinyint(4) COMMENT '是否上架',
    `discount` double(20,2) COMMENT '折扣',
    `recommend` int(11) COMMENT '是否推荐',
    `type_name` null COMMENT '',
    `user_name` null COMMENT '',
    `file_list` null COMMENT '',
    `comment_status` null COMMENT '商品评价状态',
    `num` null COMMENT '',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='电影信息实体类表';
CREATE TABLE `account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 id',
    `name` varchar(255) COMMENT '姓名(登录用户名)',
    `email` null COMMENT '邮箱',
    `password` varchar(255) COMMENT '密码',
    `level` int(11) COMMENT '权限等级',
    `sex` tinyint(4) COMMENT '性别',
    `new_password` null COMMENT '新密码',
    `address` null COMMENT '地址',
    `nick_name` varchar(255) COMMENT '昵称',
    `phone` null COMMENT '手机号',
    `account` null COMMENT '账户余额',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='用户实体类表';
CREATE TABLE `order_goods_rel` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` bigint(20) COMMENT '订单ID',
    `goods_id` bigint(20) COMMENT '商品ID',
    `count` int(11) COMMENT '商品数量',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='订单ID表';
