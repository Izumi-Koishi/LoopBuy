package com.shiwu.admin.service.impl;

import com.shiwu.admin.dao.AuditLogDao;
import com.shiwu.admin.model.DashboardStatsVO;
import com.shiwu.admin.model.StatsPeriod;
import com.shiwu.admin.service.DashboardService;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.product.dao.ProductDao;
import com.shiwu.product.model.Product;
import com.shiwu.user.dao.FollowDao;
import com.shiwu.user.dao.UserDao;

import java.time.LocalDateTime;
//import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘服务实现类 - MVC框架版本
 */
@Service
public class DashboardServiceImpl extends BaseService implements DashboardService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private FollowDao followDao;
    
    // 缓存相关
    private DashboardStatsVO cachedStats;
    private LocalDateTime lastCacheTime;
    private static final long CACHE_DURATION_MINUTES = 5; // 缓存5分钟

    // 无参构造函数，用于MVC框架依赖注入
    public DashboardServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.userDao = new UserDao();
        this.productDao = new ProductDao();
        this.auditLogDao = new AuditLogDao();
        this.followDao = new FollowDao();
        logger.info("DashboardServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public DashboardServiceImpl(UserDao userDao, ProductDao productDao,
                               AuditLogDao auditLogDao, FollowDao followDao) {
        this.userDao = userDao;
        this.productDao = productDao;
        this.auditLogDao = auditLogDao;
        this.followDao = followDao;
        logger.info("DashboardServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public DashboardStatsVO getDashboardStats() {
        // 检查缓存是否有效
        if (isCacheValid()) {
            logger.debug("返回缓存的仪表盘统计数据");
            return cachedStats;
        }
        
        // 缓存无效，重新计算
        return refreshAndGetStats();
    }
    
    @Override
    public DashboardStatsVO getDashboardStats(StatsPeriod period) {
        // 根据时间段获取统计数据（不使用缓存）
        return calculateStatsForPeriod(period);
    }
    
    @Override
    public boolean refreshStatsCache() {
        try {
            cachedStats = calculateStats();
            lastCacheTime = LocalDateTime.now();
            logger.info("仪表盘统计数据缓存刷新成功");
            return true;
        } catch (Exception e) {
            logger.error("刷新仪表盘统计数据缓存失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public DashboardStatsVO getRealTimeStats() {
        return calculateStats();
    }
    
    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid() {
        if (cachedStats == null || lastCacheTime == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesSinceCache = ChronoUnit.MINUTES.between(lastCacheTime, now);
        return minutesSinceCache < CACHE_DURATION_MINUTES;
    }
    
    /**
     * 刷新缓存并返回统计数据
     */
    private DashboardStatsVO refreshAndGetStats() {
        try {
            cachedStats = calculateStats();
            lastCacheTime = LocalDateTime.now();
            logger.debug("仪表盘统计数据已刷新");
            return cachedStats;
        } catch (Exception e) {
            logger.error("计算仪表盘统计数据失败: {}", e.getMessage(), e);
            // 如果计算失败，返回空的统计数据
            return createEmptyStats();
        }
    }
    
    /**
     * 计算统计数据
     */
    private DashboardStatsVO calculateStats() {
        logger.debug("开始计算仪表盘统计数据");
        
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setLastUpdateTime(LocalDateTime.now());
        
        try {
            // 计算总览统计
            stats.setOverview(calculateOverviewStats());
            
            // 计算用户统计
            stats.setUserStats(calculateUserStats());
            
            // 计算商品统计
            stats.setProductStats(calculateProductStats());
            
            // 计算活动统计
            stats.setActivityStats(calculateActivityStats());
            
            // 计算趋势数据
            stats.setUserTrend(calculateUserTrend(30));
            stats.setProductTrend(calculateProductTrend(30));
            stats.setActivityTrend(calculateActivityTrend(30));
            
            logger.debug("仪表盘统计数据计算完成");
            
        } catch (Exception e) {
            logger.error("计算统计数据时发生异常: {}", e.getMessage(), e);
            throw e;
        }
        
        return stats;
    }
    
    /**
     * 根据时间段计算统计数据
     */
    private DashboardStatsVO calculateStatsForPeriod(StatsPeriod period) {
        // 这里可以根据不同的时间段进行优化计算
        // 目前先使用默认计算方法
        return calculateStats();
    }
    
    /**
     * 计算总览统计数据
     */
    private DashboardStatsVO.OverviewStats calculateOverviewStats() {
        DashboardStatsVO.OverviewStats overview = new DashboardStatsVO.OverviewStats();
        
        try {
            // 获取用户相关统计
            overview.setTotalUsers(userDao.getTotalUserCount());
            overview.setTotalActiveUsers(userDao.getActiveUserCount());
            overview.setAverageRating(userDao.getAverageRating());
            
            // 获取商品相关统计
            overview.setTotalProducts(productDao.getTotalProductCount());
            overview.setTotalPendingProducts(productDao.getProductCountByStatus(Product.STATUS_PENDING_REVIEW));
            
            // 获取关注关系统计
            overview.setTotalFollowRelations(followDao.getTotalFollowCount());
            
        } catch (Exception e) {
            logger.error("计算总览统计数据失败: {}", e.getMessage(), e);
            // 设置默认值
            overview.setTotalUsers(0L);
            overview.setTotalProducts(0L);
            overview.setTotalActiveUsers(0L);
            overview.setTotalPendingProducts(0L);
            overview.setAverageRating(0.0);
            overview.setTotalFollowRelations(0L);
        }
        
        return overview;
    }
    
    /**
     * 计算用户统计数据
     */
    private DashboardStatsVO.UserStats calculateUserStats() {
        DashboardStatsVO.UserStats userStats = new DashboardStatsVO.UserStats();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            LocalDateTime weekStart = todayStart.minusDays(7);
            LocalDateTime monthStart = todayStart.minusDays(30);
            LocalDateTime lastMonthStart = todayStart.minusDays(60);
            
            // 计算新增用户数
            userStats.setNewUsersToday(userDao.getNewUserCount(todayStart, now));
            userStats.setNewUsersThisWeek(userDao.getNewUserCount(weekStart, now));
            userStats.setNewUsersThisMonth(userDao.getNewUserCount(monthStart, now));
            
            // 计算用户状态统计
            userStats.setBannedUsers(userDao.getUserCountByStatus(1)); // 1-已封禁
            userStats.setMutedUsers(userDao.getUserCountByStatus(2));   // 2-已禁言
            
            // 计算用户增长率（月环比）
            Long thisMonthUsers = userDao.getNewUserCount(monthStart, now);
            Long lastMonthUsers = userDao.getNewUserCount(lastMonthStart, monthStart);
            Double growthRate = calculateGrowthRate(thisMonthUsers, lastMonthUsers);
            userStats.setUserGrowthRate(growthRate);
            
        } catch (Exception e) {
            logger.error("计算用户统计数据失败: {}", e.getMessage(), e);
            // 设置默认值
            setDefaultUserStats(userStats);
        }
        
        return userStats;
    }

    /**
     * 计算商品统计数据
     */
    private DashboardStatsVO.ProductStats calculateProductStats() {
        DashboardStatsVO.ProductStats productStats = new DashboardStatsVO.ProductStats();

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            LocalDateTime weekStart = todayStart.minusDays(7);
            LocalDateTime monthStart = todayStart.minusDays(30);
            LocalDateTime lastMonthStart = todayStart.minusDays(60);

            // 计算新增商品数
            productStats.setNewProductsToday(productDao.getNewProductCount(todayStart, now));
            productStats.setNewProductsThisWeek(productDao.getNewProductCount(weekStart, now));
            productStats.setNewProductsThisMonth(productDao.getNewProductCount(monthStart, now));

            // 计算商品状态统计
            productStats.setOnSaleProducts(productDao.getProductCountByStatus(Product.STATUS_ONSALE));
            productStats.setSoldProducts(productDao.getProductCountByStatus(Product.STATUS_SOLD));
            productStats.setRemovedProducts(productDao.getProductCountByStatus(Product.STATUS_DELISTED));

            // 计算商品增长率（月环比）
            Long thisMonthProducts = productDao.getNewProductCount(monthStart, now);
            Long lastMonthProducts = productDao.getNewProductCount(lastMonthStart, monthStart);
            Double growthRate = calculateGrowthRate(thisMonthProducts, lastMonthProducts);
            productStats.setProductGrowthRate(growthRate);

        } catch (Exception e) {
            logger.error("计算商品统计数据失败: {}", e.getMessage(), e);
            // 设置默认值
            setDefaultProductStats(productStats);
        }

        return productStats;
    }

    /**
     * 计算活动统计数据
     */
    private DashboardStatsVO.ActivityStats calculateActivityStats() {
        DashboardStatsVO.ActivityStats activityStats = new DashboardStatsVO.ActivityStats();

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

            // 计算今日活动统计
            activityStats.setAdminLoginCount(auditLogDao.getAdminLoginCount(todayStart, now));
            activityStats.setAuditLogCount(auditLogDao.getAuditLogCount(todayStart, now));
            activityStats.setSystemErrors(auditLogDao.getSystemErrorCount(todayStart, now));

            // 模拟用户登录次数（实际应该从用户登录日志中获取）
            activityStats.setUserLoginCount(0L);

            // 模拟系统正常运行时间（实际应该从系统监控中获取）
            activityStats.setSystemUptime(99.9);

        } catch (Exception e) {
            logger.error("计算活动统计数据失败: {}", e.getMessage(), e);
            // 设置默认值
            setDefaultActivityStats(activityStats);
        }

        return activityStats;
    }

    /**
     * 计算用户趋势数据
     */
    private List<DashboardStatsVO.TrendData> calculateUserTrend(int days) {
        List<DashboardStatsVO.TrendData> trendData = new ArrayList<>();

        try {
            List<Map<String, Object>> rawData = userDao.getUserGrowthTrend(days);
            for (Map<String, Object> data : rawData) {
                String date = (String) data.get("date");
                Long count = (Long) data.get("count");
                trendData.add(new DashboardStatsVO.TrendData(date, count, "新增用户"));
            }
        } catch (Exception e) {
            logger.error("计算用户趋势数据失败: {}", e.getMessage(), e);
        }

        return trendData;
    }

    /**
     * 计算商品趋势数据
     */
    private List<DashboardStatsVO.TrendData> calculateProductTrend(int days) {
        List<DashboardStatsVO.TrendData> trendData = new ArrayList<>();

        try {
            List<Map<String, Object>> rawData = productDao.getProductGrowthTrend(days);
            for (Map<String, Object> data : rawData) {
                String date = (String) data.get("date");
                Long count = (Long) data.get("count");
                trendData.add(new DashboardStatsVO.TrendData(date, count, "新增商品"));
            }
        } catch (Exception e) {
            logger.error("计算商品趋势数据失败: {}", e.getMessage(), e);
        }

        return trendData;
    }

    /**
     * 计算活动趋势数据
     */
    private List<DashboardStatsVO.TrendData> calculateActivityTrend(int days) {
        List<DashboardStatsVO.TrendData> trendData = new ArrayList<>();

        try {
            List<Map<String, Object>> rawData = auditLogDao.getActivityTrend(days);
            for (Map<String, Object> data : rawData) {
                String date = (String) data.get("date");
                Long count = (Long) data.get("count");
                trendData.add(new DashboardStatsVO.TrendData(date, count, "系统活动"));
            }
        } catch (Exception e) {
            logger.error("计算活动趋势数据失败: {}", e.getMessage(), e);
        }

        return trendData;
    }

    /**
     * 计算增长率
     * @param current 当前值
     * @param previous 之前值
     * @return 增长率（百分比）
     */
    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100.0 : 0.0;
        }

        if (current == null) {
            return -100.0;
        }

        return ((double) (current - previous) / previous) * 100.0;
    }

    /**
     * 创建空的统计数据
     */
    private DashboardStatsVO createEmptyStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setLastUpdateTime(LocalDateTime.now());

        // 设置空的总览统计
        DashboardStatsVO.OverviewStats overview = new DashboardStatsVO.OverviewStats();
        setDefaultOverviewStats(overview);
        stats.setOverview(overview);

        // 设置空的用户统计
        DashboardStatsVO.UserStats userStats = new DashboardStatsVO.UserStats();
        setDefaultUserStats(userStats);
        stats.setUserStats(userStats);

        // 设置空的商品统计
        DashboardStatsVO.ProductStats productStats = new DashboardStatsVO.ProductStats();
        setDefaultProductStats(productStats);
        stats.setProductStats(productStats);

        // 设置空的活动统计
        DashboardStatsVO.ActivityStats activityStats = new DashboardStatsVO.ActivityStats();
        setDefaultActivityStats(activityStats);
        stats.setActivityStats(activityStats);

        // 设置空的趋势数据
        stats.setUserTrend(new ArrayList<>());
        stats.setProductTrend(new ArrayList<>());
        stats.setActivityTrend(new ArrayList<>());

        return stats;
    }

    /**
     * 设置默认的总览统计数据
     */
    private void setDefaultOverviewStats(DashboardStatsVO.OverviewStats overview) {
        overview.setTotalUsers(0L);
        overview.setTotalProducts(0L);
        overview.setTotalActiveUsers(0L);
        overview.setTotalPendingProducts(0L);
        overview.setAverageRating(0.0);
        overview.setTotalFollowRelations(0L);
    }

    /**
     * 设置默认的用户统计数据
     */
    private void setDefaultUserStats(DashboardStatsVO.UserStats userStats) {
        userStats.setNewUsersToday(0L);
        userStats.setNewUsersThisWeek(0L);
        userStats.setNewUsersThisMonth(0L);
        userStats.setBannedUsers(0L);
        userStats.setMutedUsers(0L);
        userStats.setUserGrowthRate(0.0);
    }

    /**
     * 设置默认的商品统计数据
     */
    private void setDefaultProductStats(DashboardStatsVO.ProductStats productStats) {
        productStats.setNewProductsToday(0L);
        productStats.setNewProductsThisWeek(0L);
        productStats.setNewProductsThisMonth(0L);
        productStats.setOnSaleProducts(0L);
        productStats.setSoldProducts(0L);
        productStats.setRemovedProducts(0L);
        productStats.setProductGrowthRate(0.0);
    }

    /**
     * 设置默认的活动统计数据
     */
    private void setDefaultActivityStats(DashboardStatsVO.ActivityStats activityStats) {
        activityStats.setAdminLoginCount(0L);
        activityStats.setAuditLogCount(0L);
        activityStats.setUserLoginCount(0L);
        activityStats.setSystemErrors(0L);
        activityStats.setSystemUptime(0.0);
    }
}
