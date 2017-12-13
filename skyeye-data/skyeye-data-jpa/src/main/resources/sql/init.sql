drop database if exists `monitor-center`;
create database `monitor-center`;
use `monitor-center`;

--
-- table structure for table `app_info`
--
//表示一个app应用表,包含应用所在的host、应用名字、应用类型、部署在哪个路径下、此时app的状态
//type表示历史上线的app,以及正在运行中的app  状态status也是表示history还有running
drop table if exists `app_info`;
create table `app_info` (
  `app` varchar(255) not null,
  `host` varchar(255) not null,
  `type` int(11) not null,
  `deploy` varchar(255) not null,
  `status` varchar(255) not null,
  primary key (`app`,`host`,`type`)
) engine=innodb default charset=utf8;

--
-- Table structure for table `name_info`
--
//name表示一个url 或者 第三方名字  或者 帐号名字,类型表示是api、还是带有账户的api,还是第三方请求,app表示产生该日志的app是哪个app
//用于记录哪个app产生了哪些类型的外部请求,用于web页面进行用户筛选,展示每一个app下,每一个name下产生的统计值
DROP TABLE IF EXISTS `name_info`;
CREATE TABLE `name_info` (
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `app` varchar(255) NOT NULL,
  PRIMARY KEY (`name`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `service_info`
--
//表示一个服务,一个服务是由一个服务接口以及该接口下一个方法提供的,为每一个方法分配一个唯一的ID
DROP TABLE IF EXISTS `service_info`;
CREATE TABLE `service_info` (
  `iface` varchar(255) NOT NULL,
  `method` varchar(255) NOT NULL,
  `sid` varchar(255) NOT NULL,
  PRIMARY KEY (`iface`, `method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
