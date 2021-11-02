package com.docusign.core.controller;

import com.docusign.DSConfiguration;
import com.docusign.common.ApiIndex;
import com.docusign.core.exception.LauncherException;
import com.docusign.core.model.*;
import com.docusign.core.utils.AccountsConverter;
import com.docusign.esign.client.auth.OAuth;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.docusign.core.controller.AbstractController.ERROR_PAGE;

/**
 * This class provides common model attributes for all pages. If you want to
 * refresh tokens use the next code to access to expiration time:
 * <pre>
 * {@literal @}Autowired
 * private TokenStore tokenStore;
 * ....
 * Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 * OAuth2Authentication oauth = (OAuth2Authentication) authentication;
 * OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) oauth.getDetails();
 * String accessToken = details.getTokenValue();
 * OAuth2AccessToken token = tokenStore.readAccessToken(accessToken);
 * </pre>
 * Object <code>token</code> contains all necessary information.
 */
@ControllerAdvice
@Scope(WebApplicationContext.SCOPE_SESSION)
public class GlobalControllerAdvice {

    private static final String ERROR_ACCOUNT_NOT_FOUND = "Could not find account information for the user";

    private final DSConfiguration config;
    private final Session session;
    private final User user;
    private Optional<OAuth.Account> account;
    private final AuthType authTypeSelected = AuthType.AGC;

    @Autowired
    public GlobalControllerAdvice(DSConfiguration config, Session session, User user, Optional<OAuth.Account> account) {
        this.config = config;
        this.session = session;
        this.user = user;
        this.account = account;
    }

    @ModelAttribute("authTypes")
    public List<AuthTypeItem> authTypes() {
        return AuthTypeItem.list();
    }

    @ModelAttribute("authTypeSelected")
    public AuthTypeItem authTypeSelected() {
        return AuthTypeItem.convert(authTypeSelected);
    }

    @ModelAttribute("documentOptions")
    public List<JSONObject> populateDocumentOptions() {
        return new ArrayList<>();
    }

    @ModelAttribute("showDoc")
    public boolean populateShowDoc() {
        return StringUtils.isNotBlank(config.getDocumentationPath());
    }

    @ModelAttribute("locals")
    public Locals populateLocals(ModelMap model) throws LauncherException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String selectedApi = config.getApiName().getFirstSelectedApi();
        if (!EnumUtils.isValidEnum(ApiIndex.class, selectedApi)){
            handleException("Wrong api name", selectedApi, model);
            return null;
        }
        ApiIndex apiIndex = ApiIndex.valueOf(selectedApi);
        session.setApiIndexPath(apiIndex.toString());

        if (!(authentication instanceof OAuth2Authentication)) {
            return new Locals(config, session, null, "");
        }

        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) oauth.getDetails();
        Authentication oauthUser = oauth.getUserAuthentication();

        if (oauthUser != null && oauthUser.isAuthenticated()) {
            user.setName(oauthUser.getName());
            user.setAccessToken(details.getTokenValue());

            if (account.isEmpty()) {
                account = Optional.ofNullable(getDefaultAccountInfo(getOAuthAccounts(oauth)));
            }

            OAuth.Account oauthAccount = account.orElseThrow(() -> new NoSuchElementException(ERROR_ACCOUNT_NOT_FOUND));
            session.setAccountId(oauthAccount.getAccountId());
            session.setAccountName(oauthAccount.getAccountName());
            //TODO set this more efficiently with more APIs as they're added in
            String basePath = this.getBaseUrl(apiIndex, oauthAccount) + apiIndex.getBaseUrlSuffix();
            session.setBasePath(basePath);
        }

        return new Locals(config, session, user, "");
    }

    @PostMapping
    private String handleException(String errorName, String errorMethod, ModelMap model) {
        new DoneExample()
                .withTitle(errorName)
                .withName(errorName)
                .withMessage(errorMethod)
                .addToModel(model);
        return ERROR_PAGE;
    }

    private String getBaseUrl(ApiIndex apiIndex, OAuth.Account oauthAccount) {
        if (apiIndex.equals(ApiIndex.ROOMS)) {
            return this.config.getRoomsBasePath();
        } else if (apiIndex.equals(ApiIndex.CLICK)) {
            return this.config.getClickBasePath();
        }  else if (apiIndex.equals(ApiIndex.MONITOR)) {
            return this.config.getMonitorBasePath();
        }  else if (apiIndex.equals(ApiIndex.ADMIN)) {
            return this.config.getAdminBasePath();
        } else {
            return oauthAccount.getBaseUri();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<OAuth.Account> getOAuthAccounts(OAuth2Authentication oauth) {
        Map<?, ?> userAuthenticationDetails = (Map<?, ?>) oauth.getUserAuthentication().getDetails();
        List<Map<String, Object>> oauthAccounts = (ArrayList<Map<String, Object>>) userAuthenticationDetails.get("accounts");
        return oauthAccounts.stream()
            .map(AccountsConverter::convert)
            .collect(Collectors.toList());
    }

    private OAuth.Account getDefaultAccountInfo(List<OAuth.Account> accounts) {
        String targetAccountId = config.getTargetAccountId();
        if (StringUtils.isNotBlank(targetAccountId)) {
            OAuth.Account account = getAccountById(accounts, targetAccountId);
            if(account != null) {
                return account;
            }
        }
        return getDefaultAccount(accounts);
    }

    private static OAuth.Account getDefaultAccount(List<OAuth.Account> accounts) {
        for (OAuth.Account oauthAccount : accounts) {
            if ("true".equals(oauthAccount.getIsDefault())) {
                return oauthAccount;
            }
        }
        return null;
    }

    private static OAuth.Account getAccountById(List<OAuth.Account> accounts, String accountId) {
        for (OAuth.Account oauthAccount : accounts) {
            if (StringUtils.equals(oauthAccount.getAccountId(), accountId)) {
                return oauthAccount;
            }
        }
        return null;
    }
}
