# user-service-core

需要使用user-service的引入此项目即可

web.xml配置:

<!-- Permissions Filter -->
<filter>
	<filter-name>PermissionsFilter</filter-name>
	<filter-class>org.blazer.userservice.core.filter.PermissionsFilter</filter-class>
	<!-- 是否启用过滤器开关，0：关，1：开 -->
	<init-param>
		<param-name>on-off</param-name>
		<param-value>1</param-value>
	</init-param>
	<!-- 注册的系统名称，必填 -->
	<init-param>
		<param-name>systemName</param-name>
		<param-value>user-service</param-value>
	</init-param>
	<!-- serviceUrl访问的服务URL，必填，innerServiceUrl仅仅用于内部访问，即应用服务和用户服务在同一个机房，不走公网网络可以增加效率，当然也可以配到HOST解决 -->
	<init-param>
		<param-name>serviceUrl</param-name>
		<param-value>http://bigdata.blazer.org:8030</param-value>
	</init-param>
	<!-- innerServiceUrl不配置默认应用serviceUrl的值 -->
	<init-param>
		<param-name>innerServiceUrl</param-name>
		<param-value>http://bigdata.blazer.org:8030</param-value>
	</init-param>
	<!-- 没有权限时的显示页面 -->
	<init-param>
		<param-name>noPermissionsPage</param-name>
		<param-value>nopermissions.html</param-value>
	</init-param>
	<!-- 用户登录存活周期，单位是秒 -->
	<init-param>
		<param-name>cookieSeconds</param-name>
		<param-value>1500</param-value>
	</init-param>
	<!-- 过滤url -->
	<init-param>
		<param-name>ignoreUrls</param-name>
		<param-value>/userservice/*</param-value>
	</init-param>
	<!-- js模板生成 -->
	<init-param>
		<param-name>templateJs</param-name>
		<param-value>/js/userservice_template.js:/js/userservice.js,/js/domainhelp_template.js:/js/domainhelp.js</param-value>
	</init-param>
</filter>
<filter-mapping>
	<filter-name>PermissionsFilter</filter-name>
	<url-pattern>*.do</url-pattern>
</filter-mapping>
<filter-mapping>
	<filter-name>PermissionsFilter</filter-name>
	<url-pattern>*.html</url-pattern>
</filter-mapping>
