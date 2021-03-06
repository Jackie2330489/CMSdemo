package com.niit.cmsdemo.config;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import com.github.pagehelper.util.StringUtil;
import com.niit.cmsdemo.domain.Permission;
import com.niit.cmsdemo.service.PermissionService;
import com.niit.cmsdemo.shiro.MyShiroRealm;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.shiro.mgt.SecurityManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ShiroConfig {
    @Autowired(required = false)
    private PermissionService permissionService;

    @Bean
    public static LifecycleBeanPostProcessor getLifecycleBeanPostProcessor(){
        return new LifecycleBeanPostProcessor();
    }

    /**
     * ShiroDialect,为了在thymeleaf里使用shiro的标签bean
     */
    @Bean
    public ShiroDialect shiroDialect(){
        return  new ShiroDialect();
    }
    /**
     * ShiroFiterFactoryBean处理拦截资源文件问题。
     * 注意：单独一个ShiroFilerFactoryBean配置是或报错的，因为在
     * 初始化ShiroFilterFactoryBean的时候需要注入：SecurityManager
     *
     * Filter Chain定义说明
     * 1.一个url可以配置多个Filer，使用逗号分隔
     * 2.当设置多个过滤器时，全部验证通过，才视为通过
     * 3.部分过滤器可制定参数，如perms，roles
     *
     */
    @Bean
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager){

        System.out.println("ShiroConfiguration.shirFilter()");
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();

        //必须设置SecurityManager
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        //如果不设置默认会自动寻找web工程根目录下的"/login.jsp"页面
        shiroFilterFactoryBean.setLoginUrl("/login");
        //登录成功后要跳转的链接
        shiroFilterFactoryBean.setSuccessUrl("/manage");
        //未授权界面
        shiroFilterFactoryBean.setUnauthorizedUrl("/403");
        //拦截器
        Map<String,String> filterChainDefinitionMap = new LinkedHashMap<String,String>();

        //配置退出过滤器，其中的具体的退出代码Shiro已经替我们实现了
        filterChainDefinitionMap.put("/logout","logout");
        filterChainDefinitionMap.put("/css/**","anon");
        filterChainDefinitionMap.put("/js/**","anon");
        filterChainDefinitionMap.put("/img/**","anon");
        filterChainDefinitionMap.put("/bootstrap/**","anon");
        filterChainDefinitionMap.put("/font-awesome","anon");
        //过滤链定义，从上向下顺序执行
        //authc:所有url都必须认证通过才可以访问，anon：所有的url都可以匿名访问
        //自定义加载权限资源关系
        List<Permission> permissionList = permissionService.findConditions(null);
        for (Permission permission: permissionList) {
            if(StringUtil.isNotEmpty(permission.getUrl())){
                String permi = "perms[" + permission.getUrl() + "]";
                filterChainDefinitionMap.put(permission.getUrl(),permi);
            }
        }
        filterChainDefinitionMap.put("/**","authc");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    @Bean
    public SecurityManager securityManager(){
        DefaultSecurityManager securityManager = new DefaultWebSecurityManager();
        //设置realm
        securityManager.setRealm(myShiroRealm());
        securityManager.setSessionManager(sessionManager());
        return securityManager;
    }

    @Bean
    public MyShiroRealm myShiroRealm(){
        MyShiroRealm myShiroRealm = new MyShiroRealm();
        myShiroRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        return myShiroRealm;
    }

    /**
     * 凭证匹配器
     * （由于密码校验交给Shiro的SimpleAuthenticationInfo进行处理了
     * 所以需要修改daGetAuthenticaltionInfo的代码；
     * ）
     * @return
     */
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher(){
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();

        hashedCredentialsMatcher.setHashAlgorithmName("md5");  //散列算法；
        hashedCredentialsMatcher.setHashIterations(2);//散列的次数，比如散列两次，相当于md5（md5()）；

        return  hashedCredentialsMatcher;
    }

    /**
     * 开启shiro aop注解支持
     * 使用代理方式；所以需要开启代码支持；
     * @param securityManager
     * @return
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager){
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager());
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * shiro session的管理
     */
    @Bean
    public DefaultWebSessionManager sessionManager(){
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        return sessionManager;
    }
}
