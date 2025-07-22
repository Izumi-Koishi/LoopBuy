package com.shiwu;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 应用程序启动类
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            // 创建Tomcat实例
            Tomcat tomcat = new Tomcat();

            // 设置端口
            int port = 8080;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    logger.warn("无效的端口号参数，使用默认端口8080");
                }
            }

            // 设置工作目录
            String workingDir = System.getProperty("java.io.tmpdir");
            tomcat.setBaseDir(workingDir);

            // 显式创建和配置Connector
            tomcat.setPort(port);
            tomcat.getConnector(); // 这会创建默认的HTTP Connector

            // 获取webapp目录
            String webappDirLocation = "src/main/webapp/";
            File webappDir = new File(webappDirLocation);
            if (!webappDir.exists()) {
                webappDir = new File("target/shiwu-marketplace-1.0-SNAPSHOT");
            }

            // 创建Context - 使用空字符串作为context path
            Context context = tomcat.addWebapp("", webappDir.getAbsolutePath());
            context.setParentClassLoader(Application.class.getClassLoader());

            logger.info("✅ 已配置Webapp: {}", webappDir.getAbsolutePath());

            // 手动注册AdminController
            try {
                Class<?> adminControllerClass = Class.forName("com.shiwu.admin.controller.AdminController");
                Object adminController = adminControllerClass.getDeclaredConstructor().newInstance();
                tomcat.addServlet("", "AdminController", (javax.servlet.Servlet) adminController);
                context.addServletMappingDecoded("/api/admin/*", "AdminController");
                logger.info("✅ 已注册AdminController: /api/admin/*");
            } catch (Exception e) {
                logger.warn("❌ 注册AdminController失败: {}", e.getMessage());
            }

            // 注册MVC框架的DispatcherServlet
            try {
                Class<?> dispatcherServletClass = Class.forName("com.shiwu.framework.web.DispatcherServlet");
                Object dispatcherServlet = dispatcherServletClass.getDeclaredConstructor().newInstance();
                tomcat.addServlet("", "DispatcherServlet", (javax.servlet.Servlet) dispatcherServlet);
                context.addServletMappingDecoded("/mvc/*", "DispatcherServlet");
                logger.info("✅ 已注册MVC框架DispatcherServlet: /mvc/*");
            } catch (Exception e) {
                logger.warn("❌ 注册DispatcherServlet失败: {}", e.getMessage());
            }

            // 启动Tomcat
            logger.info("正在启动Shiwu校园二手交易平台...");
            logger.info("端口: {}", port);
            logger.info("Webapp目录: {}", webappDir.getAbsolutePath());

            tomcat.start();

            logger.info("🎉 Shiwu校园二手交易平台启动成功！");
            logger.info("🌐 访问地址: http://localhost:{}", port);
            logger.info("📋 API文档: http://localhost:{}/api/", port);
            logger.info("🔐 管理员登录: http://localhost:{}/api/admin/login", port);
            logger.info("👥 用户API: http://localhost:{}/api/user/", port);
            logger.info("🛍️ 商品API: http://localhost:{}/api/products/", port);
            logger.info("📊 仪表盘: http://localhost:{}/admin/dashboard/", port);
            logger.info("📋 审计日志: http://localhost:{}/api/admin/audit-logs/", port);
            logger.info("🚀 MVC框架演示: http://localhost:{}/mvc/api/messages/", port);

            // 等待服务器关闭
            tomcat.getServer().await();

        } catch (LifecycleException e) {
            logger.error("启动Tomcat服务器失败", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            System.exit(1);
        }
    }
}