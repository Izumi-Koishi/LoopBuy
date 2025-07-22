package com.shiwu.framework.core;

import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Controller;
import com.shiwu.framework.annotation.ControllerAdvice;
import com.shiwu.framework.annotation.ExceptionHandler;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IoC容器 - 控制反转(Inversion of Control)容器
 *
 * 负责管理应用中所有Bean的生命周期、依赖注入和自动装配。
 * 这是框架的核心组件，实现了Spring的ApplicationContext的简化版本。
 *
 * <h3>核心功能：</h3>
 * <ul>
 * <li>组件扫描：自动扫描指定包下的所有组件</li>
 * <li>Bean管理：创建和管理Bean实例的生命周期</li>
 * <li>依赖注入：自动注入Bean之间的依赖关系</li>
 * <li>类型转换：支持基于类型的Bean查找</li>
 * <li>异常处理器管理：管理全局异常处理器</li>
 * </ul>
 *
 * <h3>支持的组件类型：</h3>
 * <ul>
 * <li>@Controller - 控制器组件</li>
 * <li>@Service - 服务层组件</li>
 * <li>@Repository - 数据访问层组件</li>
 * <li>@ControllerAdvice - 全局异常处理器</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 创建IoC容器，扫描指定包
 * ApplicationContext context = new ApplicationContext("com.shiwu");
 *
 * // 获取Bean实例
 * UserService userService = context.getBean(UserService.class);
 * UserController userController = context.getBean("userController");
 *
 * // 获取所有控制器
 * Map<String, Object> controllers = context.getControllers();
 * }
 * </pre>
 *
 * @author LoopBuy Framework Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);

    /**
     * Bean容器 - 存储所有管理的Bean实例
     * 键：Bean名称，值：Bean实例
     */
    private final Map<String, Object> beanContainer = new ConcurrentHashMap<>();

    /**
     * Bean类型映射 - 根据类型快速查找Bean
     * 键：Bean类型，值：Bean实例
     */
    private final Map<Class<?>, Object> beanTypeMap = new ConcurrentHashMap<>();

    /**
     * 接口实现映射 - 存储接口到实现类的映射
     * 键：接口类型，值：实现类实例
     */
    private final Map<Class<?>, Object> interfaceImplMap = new ConcurrentHashMap<>();

    /**
     * 异常处理器映射 - 存储异常类型到处理方法的映射
     * 键：异常类型，值：ExceptionHandlerMethod
     */
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> exceptionHandlers = new HashMap<>();

    /**
     * 全局异常处理器 - 存储所有@ControllerAdvice标记的类实例
     */
    private final List<Object> globalExceptionHandlers = new ArrayList<>();

    /**
     * 扫描的包路径
     */
    private final String basePackage;

    /**
     * 创建ApplicationContext并初始化
     *
     * @param basePackage 要扫描的基础包路径
     */
    public ApplicationContext(String basePackage) {
        this.basePackage = basePackage;

        // 1. 扫描并注册所有Bean
        scanAndRegisterBeans();

        // 2. 注入所有依赖
        injectDependencies();

        // 3. 注册异常处理器
        registerExceptionHandlers();

        logger.info("✅ ApplicationContext初始化完成，共管理{}个Bean", beanContainer.size());
        logger.info("✅ 注册{}个全局异常处理器", globalExceptionHandlers.size());
        logger.info("✅ 注册{}个异常处理方法", exceptionHandlers.size());
    }

    /**
     * 异常处理方法包装类
     */
    public static class ExceptionHandlerMethod {
        private final Object bean;
        private final Method method;

        public ExceptionHandlerMethod(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }

        public Object getBean() {
            return bean;
        }

        public Method getMethod() {
            return method;
        }
    }
    
    /**
     * 扫描并注册所有Bean
     * 支持@Controller、@Service、@Repository和@ControllerAdvice注解
     */
    private void scanAndRegisterBeans() {
        try {
            Set<Class<?>> classes = scanClasses(basePackage);

            for (Class<?> clazz : classes) {
                // 检查是否标记了组件注解
                if (isComponent(clazz)) {
                    // 创建Bean实例
                    String beanName = getBeanName(clazz);
                    Object instance = clazz.getDeclaredConstructor().newInstance();

                    // 注册到容器
                    beanContainer.put(beanName, instance);
                    beanTypeMap.put(clazz, instance);

                    // 记录全局异常处理器
                    if (clazz.isAnnotationPresent(ControllerAdvice.class)) {
                        globalExceptionHandlers.add(instance);
                        logger.debug("注册全局异常处理器: {}", clazz.getSimpleName());
                    }

                    // 注册接口实现
                    registerInterfaceImplementation(clazz, instance);

                    logger.debug("注册Bean: {} -> {}", beanName, clazz.getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.error("扫描Bean失败", e);
            throw new RuntimeException("IoC容器初始化失败", e);
        }
    }

    /**
     * 判断类是否是组件
     *
     * @param clazz 要检查的类
     * @return 如果是组件返回true，否则返回false
     */
    private boolean isComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(Controller.class) ||
               clazz.isAnnotationPresent(Service.class) ||
               clazz.isAnnotationPresent(Repository.class) ||
               clazz.isAnnotationPresent(ControllerAdvice.class);
    }

    /**
     * 注册接口实现
     * 将实现类注册到其实现的所有接口
     *
     * @param clazz 实现类
     * @param instance 实例
     */
    private void registerInterfaceImplementation(Class<?> clazz, Object instance) {
        // 获取类实现的所有接口
        Class<?>[] interfaces = clazz.getInterfaces();

        for (Class<?> interfaceClass : interfaces) {
            // 注册到接口映射
            interfaceImplMap.put(interfaceClass, instance);

            // 同时注册到类型映射，使getBean(Interface.class)可以工作
            if (!beanTypeMap.containsKey(interfaceClass)) {
                beanTypeMap.put(interfaceClass, instance);
                logger.debug("注册接口实现: {} -> {}",
                    interfaceClass.getSimpleName(), clazz.getSimpleName());
            } else {
                logger.warn("接口有多个实现: {} -> {}",
                    interfaceClass.getSimpleName(), clazz.getSimpleName());
            }
        }
    }
    
    /**
     * 注入所有依赖
     */
    private void injectDependencies() {
        for (Object bean : beanContainer.values()) {
            injectBean(bean);
        }
    }

    /**
     * 注册异常处理器
     * 扫描所有@ControllerAdvice类中的@ExceptionHandler方法
     */
    private void registerExceptionHandlers() {
        for (Object handler : globalExceptionHandlers) {
            Class<?> handlerClass = handler.getClass();
            Method[] methods = handlerClass.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {
                    ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
                    Class<? extends Throwable>[] exceptionTypes = annotation.value();

                    // 如果没有指定异常类型，从方法参数推断
                    if (exceptionTypes.length == 0) {
                        exceptionTypes = inferExceptionTypesFromMethod(method);
                    }

                    // 注册每个异常类型的处理方法
                    for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                        ExceptionHandlerMethod handlerMethod = new ExceptionHandlerMethod(handler, method);
                        exceptionHandlers.put(exceptionType, handlerMethod);

                        logger.debug("注册异常处理器: {} -> {}.{}",
                            exceptionType.getSimpleName(),
                            handlerClass.getSimpleName(),
                            method.getName());
                    }
                }
            }
        }
    }

    /**
     * 从方法参数推断异常类型
     *
     * @param method 异常处理方法
     * @return 异常类型数组
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] inferExceptionTypesFromMethod(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        List<Class<? extends Throwable>> exceptionTypes = new ArrayList<>();

        for (Class<?> paramType : paramTypes) {
            if (Throwable.class.isAssignableFrom(paramType)) {
                exceptionTypes.add((Class<? extends Throwable>) paramType);
            }
        }

        return exceptionTypes.toArray(new Class[0]);
    }
    
    /**
     * 为单个Bean注入依赖
     * 支持按类型注入和按接口注入
     *
     * @param bean 要注入依赖的Bean实例
     */
    private void injectBean(Object bean) {
        Class<?> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                try {
                    field.setAccessible(true);
                    Object dependency = findDependency(field.getType());

                    if (dependency != null) {
                        field.set(bean, dependency);
                        logger.debug("注入依赖: {}.{} = {}",
                            clazz.getSimpleName(), field.getName(), dependency.getClass().getSimpleName());
                    } else {
                        Autowired autowired = field.getAnnotation(Autowired.class);
                        if (autowired.required()) {
                            throw new RuntimeException("找不到依赖: " + field.getType().getName() +
                                " for field: " + clazz.getSimpleName() + "." + field.getName());
                        } else {
                            logger.debug("可选依赖未找到: {}.{}", clazz.getSimpleName(), field.getName());
                        }
                    }
                } catch (Exception e) {
                    logger.error("依赖注入失败: {}.{}", clazz.getSimpleName(), field.getName(), e);
                    throw new RuntimeException("依赖注入失败: " + clazz.getSimpleName() + "." + field.getName(), e);
                }
            }
        }
    }

    /**
     * 查找依赖Bean
     * 优先按具体类型查找，如果找不到则按接口查找
     *
     * @param type 依赖类型
     * @return 依赖Bean实例，如果找不到返回null
     */
    private Object findDependency(Class<?> type) {
        // 1. 优先按具体类型查找
        Object dependency = beanTypeMap.get(type);
        if (dependency != null) {
            return dependency;
        }

        // 2. 如果是接口，查找实现类
        if (type.isInterface()) {
            dependency = interfaceImplMap.get(type);
            if (dependency != null) {
                return dependency;
            }
        }

        // 3. 按类型的父类或接口查找
        for (Map.Entry<Class<?>, Object> entry : beanTypeMap.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }
    
    /**
     * 获取Bean名称
     *
     * @param clazz 类
     * @return Bean名称
     */
    private String getBeanName(Class<?> clazz) {
        // 1. 检查是否有自定义名称
        String customName = null;

        if (clazz.isAnnotationPresent(Controller.class)) {
            customName = clazz.getAnnotation(Controller.class).value();
        } else if (clazz.isAnnotationPresent(Service.class)) {
            customName = clazz.getAnnotation(Service.class).value();
        } else if (clazz.isAnnotationPresent(Repository.class)) {
            customName = clazz.getAnnotation(Repository.class).value();
        } else if (clazz.isAnnotationPresent(ControllerAdvice.class)) {
            customName = clazz.getAnnotation(ControllerAdvice.class).value();
        }

        // 2. 如果有自定义名称且不为空，使用自定义名称
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }

        // 3. 否则使用类名首字母小写
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 扫描指定包下的所有类
     *
     * @param packageName 包名
     * @return 类集合
     */
    private Set<Class<?>> scanClasses(String packageName) throws Exception {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(packagePath);

        if (resource != null) {
            File packageDir = new File(resource.getFile());
            scanDirectory(packageDir, packageName, classes);
        }

        return classes;
    }

    /**
     * 递归扫描目录
     *
     * @param dir 目录
     * @param packageName 包名
     * @param classes 类集合
     */
    private void scanDirectory(File dir, String packageName, Set<Class<?>> classes) throws Exception {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    logger.warn("无法加载类: {}", className);
                }
            }
        }
    }

    /**
     * 根据名称获取Bean
     *
     * @param name Bean名称
     * @return Bean实例
     */
    public Object getBean(String name) {
        return beanContainer.get(name);
    }

    /**
     * 根据类型获取Bean
     *
     * @param type Bean类型
     * @return Bean实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        Object bean = beanTypeMap.get(type);

        // 如果直接找不到，尝试按接口查找
        if (bean == null && type.isInterface()) {
            bean = interfaceImplMap.get(type);
        }

        // 如果还找不到，尝试按类型兼容性查找
        if (bean == null) {
            for (Map.Entry<Class<?>, Object> entry : beanTypeMap.entrySet()) {
                if (type.isAssignableFrom(entry.getKey())) {
                    return (T) entry.getValue();
                }
            }
        }

        return (T) bean;
    }

    /**
     * 获取所有Controller Bean
     *
     * @return 控制器映射
     */
    public Map<String, Object> getControllers() {
        Map<String, Object> controllers = new HashMap<>();
        for (Map.Entry<String, Object> entry : beanContainer.entrySet()) {
            if (entry.getValue().getClass().isAnnotationPresent(Controller.class)) {
                controllers.put(entry.getKey(), entry.getValue());
            }
        }
        return controllers;
    }

    /**
     * 获取所有全局异常处理器
     *
     * @return 全局异常处理器列表
     */
    public List<Object> getGlobalExceptionHandlers() {
        return new ArrayList<>(globalExceptionHandlers);
    }

    /**
     * 查找异常处理方法
     *
     * @param exceptionType 异常类型
     * @return 异常处理方法，如果找不到返回null
     */
    public ExceptionHandlerMethod findExceptionHandler(Class<? extends Throwable> exceptionType) {
        // 1. 直接匹配
        ExceptionHandlerMethod handler = exceptionHandlers.get(exceptionType);
        if (handler != null) {
            return handler;
        }

        // 2. 按继承关系匹配
        Class<?> currentType = exceptionType;
        while (currentType != null && currentType != Throwable.class) {
            handler = exceptionHandlers.get(currentType);
            if (handler != null) {
                return handler;
            }
            currentType = currentType.getSuperclass();
        }

        // 3. 最后尝试匹配Exception或Throwable
        handler = exceptionHandlers.get(Exception.class);
        if (handler != null) {
            return handler;
        }

        return exceptionHandlers.get(Throwable.class);
    }

    /**
     * 获取所有Bean
     *
     * @return Bean容器的副本
     */
    public Map<String, Object> getAllBeans() {
        return new HashMap<>(beanContainer);
    }

    /**
     * 获取Bean数量
     *
     * @return Bean数量
     */
    public int getBeanCount() {
        return beanContainer.size();
    }

    /**
     * 检查Bean是否存在
     *
     * @param name Bean名称
     * @return 如果存在返回true，否则返回false
     */
    public boolean containsBean(String name) {
        return beanContainer.containsKey(name);
    }

    /**
     * 检查Bean类型是否存在
     *
     * @param type Bean类型
     * @return 如果存在返回true，否则返回false
     */
    public boolean containsBeanType(Class<?> type) {
        return beanTypeMap.containsKey(type) || interfaceImplMap.containsKey(type);
    }
}
