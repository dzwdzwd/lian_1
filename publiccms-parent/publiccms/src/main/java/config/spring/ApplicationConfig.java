package config.spring;

import static config.initializer.InitializationInitializer.CMS_CONFIG_FILE;
import static org.publiccms.common.constants.CommonConstants.CMS_FILEPATH;
import static java.lang.Integer.parseInt;
import static org.publiccms.common.database.CmsDataSource.DATABASE_CONFIG_FILENAME;
import static org.springframework.core.io.support.PropertiesLoaderUtils.loadAllProperties;
import static org.springframework.scheduling.quartz.SchedulerFactoryBean.PROP_THREAD_COUNT;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.publiccms.common.base.AbstractFreemarkerView;
import org.publiccms.common.constants.CmsVersion;
import org.publiccms.common.database.CmsDataSource;
import org.publiccms.common.search.MultiTokenizerFactory;
import org.publiccms.logic.component.site.DirectiveComponent;
import org.publiccms.logic.component.site.SiteComponent;
import org.publiccms.logic.component.template.TemplateComponent;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.publiccms.common.base.Base;
import com.publiccms.common.cache.CacheEntityFactory;

/**
 *
 * Spring?????????
 *
 * Spring Configuration Class
 *
 */
@Configuration
@ComponentScan(basePackages = "org.publiccms", excludeFilters = { @ComponentScan.Filter(value = { Controller.class }) })
@MapperScan(basePackages = "org.publiccms.logic.mapper")
@PropertySource({ "classpath:" + CMS_CONFIG_FILE })
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig implements Base {

    @Autowired
    private Environment env;

    /**
     * ?????????
     *
     * data source
     *
     * @return
     * @throws PropertyVetoException
     */
    @Bean
    public DataSource dataSource() throws PropertyVetoException {
        CmsDataSource bean = new CmsDataSource(getDirPath("") + DATABASE_CONFIG_FILENAME);
        // try {
        // bean.put("other",
        // CmsDataSource.initDataSource(loadAllProperties("config/database-other.properties")));
        // } catch (IOException e1) {
        // e1.printStackTrace();
        // }
        if (CmsVersion.isInitialized()) {
            try {
                CmsDataSource.initDefautlDataSource();
            } catch (IOException | PropertyVetoException e) {
                CmsVersion.setInitialized(false);
            }
        }
        return bean;
    }

    /**
     * Hibernate ????????????
     *
     * Hibernate Transaction Manager
     *
     * @param sessionFactory
     * @return
     */
    @Bean
    public HibernateTransactionManager hibernateTransactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager bean = new HibernateTransactionManager();
        bean.setSessionFactory(sessionFactory);
        return bean;
    }

    /**
     * Mybatis????????????
     *
     * Mybatis Session Factory
     *
     * @param dataSource
     * @return
     * @throws IOException
     */
    @Bean
    public SqlSessionFactoryBean mybatisSqlSessionFactoryBean(DataSource dataSource) throws IOException {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setCacheEnabled(true);
        configuration.setLazyLoadingEnabled(false);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        bean.setMapperLocations(resolver.getResources("classpath*:mapper/**/*Mapper.xml"));
        bean.setConfiguration(configuration);
        return bean;
    }

    /**
     * Hibernate ???????????????
     *
     * Hibernate Session Factory
     *
     * @param dataSource
     * @return
     * @throws PropertyVetoException
     * @throws IOException
     */
    @Bean
    public FactoryBean<SessionFactory> hibernateSessionFactory(DataSource dataSource) throws PropertyVetoException, IOException {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setPackagesToScan("org.publiccms.entities");
        MultiTokenizerFactory.setName(env.getProperty("cms.tokenizerFactory"));
        Properties properties = loadAllProperties(env.getProperty("cms.hibernate.configFilePath"));
        properties.setProperty("hibernate.search.default.indexBase", getDirPath("/indexes/"));
        bean.setHibernateProperties(properties);
        return bean;
    }

    /**
     * ????????????
     *
     * Cache Factory
     *
     * @return
     * @throws IOException
     */
    @Bean
    public CacheEntityFactory cacheEntityFactory() throws IOException {
        CacheEntityFactory bean = new CacheEntityFactory(env.getProperty("cms.cache.configFilePath"));
        return bean;
    }

    /**
     * ???????????????
     *
     * Internationalization
     *
     * @return
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource bean = new ResourceBundleMessageSource();
        bean.setBasenames(new String[] { "language.message", "language.config", "language.operate" });
        bean.setCacheSeconds(300);
        bean.setUseCodeAsDefaultMessage(true);
        return bean;
    }

    /**
     * ????????????
     *
     * Directive Component
     *
     * @return
     */
    @Bean
    public DirectiveComponent directiveComponent() {
        DirectiveComponent bean = new DirectiveComponent();
        bean.setDirectiveRemoveRegex(env.getProperty("cms.directiveRemoveRegex"));
        bean.setMethodRemoveRegex(env.getProperty("cms.methodRemoveRegex"));
        return bean;
    }

    /**
     * ????????????
     *
     * Template Component
     *
     * @return
     */
    @Bean
    public TemplateComponent templateComponent() {
        TemplateComponent bean = new TemplateComponent();
        bean.setDirectivePrefix(env.getProperty("cms.directivePrefix"));
        return bean;
    }

    /**
     * ????????????
     *
     * Site Component
     *
     * @return
     */
    @Bean
    public SiteComponent siteComponent() {
        SiteComponent bean = new SiteComponent();
        bean.setRootPath(getDirPath(""));
        bean.setSiteMasters(env.getProperty("cms.masterSiteIds"));
        bean.setDefaultSiteId(parseInt(env.getProperty("cms.defaultSiteId")));
        return AbstractFreemarkerView.siteComponent = bean;
    }

    /**
     * FreeMarker????????????
     *
     * FreeMarker Configuration Factory
     *
     * @return
     * @throws IOException
     */
    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() throws IOException {
        FreeMarkerConfigurer bean = new FreeMarkerConfigurer();
        bean.setTemplateLoaderPath("classpath:/templates/");
        Properties properties = loadAllProperties(env.getProperty("cms.freemarker.configFilePath"));
        bean.setFreemarkerSettings(properties);
        return bean;
    }

    /**
     * 
     * ??????????????????
     * 
     * Task Scheduler Factory
     * 
     * @return
     */
    @Bean
    public SchedulerFactoryBean scheduler() {
        SchedulerFactoryBean bean = new SchedulerFactoryBean();
        Properties properties = new Properties();
        properties.setProperty(PROP_THREAD_COUNT, env.getProperty("cms.task.threadCount"));
        bean.setQuartzProperties(properties);
        return bean;
    }

    /**
     * ????????????????????????
     *
     * File Upload Resolver
     *
     * @return
     */
    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver bean = new CommonsMultipartResolver();
        bean.setDefaultEncoding(DEFAULT_CHARSET_NAME);
        bean.setMaxUploadSize(Long.parseLong(env.getProperty("cms.multipart.maxUploadSize")));
        return bean;
    }

    /**
     * json???Jsonp????????????????????????????????????RequestBody???ResponseBody
     *
     * Json???Jsonp Message Converter , Support For RequestBody???ResponseBody
     *
     * @return
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        return new MappingJackson2HttpMessageConverter();
    }

    private String getDirPath(String path) {
        String filePath = CMS_FILEPATH + path;
        File dir = new File(filePath);
        dir.mkdirs();
        return dir.getAbsolutePath();
    }
}