package com.docusign.controller.eSignature.examples;

import com.docusign.DSConfiguration;
import com.docusign.common.WorkArguments;
import com.docusign.controller.eSignature.services.SharedAccessService;
import com.docusign.core.model.DoneExample;
import com.docusign.core.model.Session;
import com.docusign.core.model.User;
import com.docusign.esign.api.AccountsApi;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.api.UsersApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.EnvelopesInformation;
import com.docusign.esign.model.NewUsersSummary;
import com.docusign.esign.model.UserInformation;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

import static com.docusign.controller.eSignature.examples.EG043ControllerSharedAccess.EG_043;

@Controller
@RequestMapping(EG_043)
public class EG043ControllerSharedAccess extends AbstractEsignatureController {
    public static final String EG_043 = "/eg043";
    public static final String ACTIVATE_USER = "/activateUser";
    public static final String LOGOUT = "/logout";
    public static final String LIST_ENVELOPES = "/listEnvelopes";
    public static final String ACT_ON_BEHALF = "X-DocuSign-Act-On-Behalf";
    public static final String HOME_PAGE = "/";
    private final Session session;
    private final User user;

    @Autowired
    public EG043ControllerSharedAccess(DSConfiguration config, Session session, User user) {
        super(config, EG_043);
        this.session = session;
        this.user = user;
    }

    @Override
    protected Object doWork(WorkArguments args, ModelMap model,
                            HttpServletResponse response) throws Exception {
        String basePath = session.getBasePath();
        String accessToken = user.getAccessToken();

        UsersApi usersApi = createUsersApi(basePath, accessToken);

        UserInformation userInformation = (new SharedAccessService()).checkIfAgentExists(
                usersApi,
                session.getAccountId(),
                args.getAgentEmail()
        );

        if (userInformation == null) {
            NewUsersSummary usersSummary = (new SharedAccessService()).createAgent(
                    usersApi,
                    session.getAccountId(),
                    args.getAgentEmail(),
                    args.getAgentName(),
                    args.getActivationCode());

            this.session.setCreatedUserId(usersSummary.getNewUsers().get(0).getUserId());

            DoneExample.createDefault("Agent user created")
                    .withMessage(getTextForCodeExampleByApiType().ResultsPageText)
                    .withJsonObject(usersSummary)
                    .withRedirect(EG_043 + ACTIVATE_USER)
                    .addToModel(model, config);
        } else {
            this.session.setCreatedUserId(userInformation.getUserId());

            DoneExample.createDefault("Agent user created")
                    .withMessage(getTextForCodeExampleByApiType().ResultsPageText)
                    .withJsonObject(userInformation)
                    .withRedirect(EG_043 + ACTIVATE_USER)
                    .addToModel(model, config);
        }

        return DONE_EXAMPLE_PAGE;
    }

    @RequestMapping(value = ACTIVATE_USER, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object activateUser(ModelMap model) throws ApiException {
        String basePath = session.getBasePath();
        String accessToken = user.getAccessToken();

        AccountsApi accountsApi = createAccountsApi(basePath, accessToken);
        OAuth.UserInfo user = getCurrentUserId(basePath, accessToken);
        String userId = user.getSub();

        try {
            (new SharedAccessService()).activateAgent(
                    accountsApi,
                    session.getAccountId(),
                    userId,
                    session.getCreatedUserId());

            this.config.setPrincipalUserId(userId);
            this.config.setShareAccessExampleScenario(true);
            this.config.setAdditionalRedirect(true);

            DoneExample.createDefault("Authenticate as the agent")
                    .withMessage(getTextForCodeExampleByApiType().getAdditionalPage().get(0).ResultsPageText)
                    .withRedirect(LOGOUT)
                    .addToModel(model, config);
        } catch(ApiException exception) {
            DoneExample.createDefault("Authenticate as the agent")
                    .withMessage(getTextForCodeExampleByApiType().getAdditionalPage().get(3).ResultsPageText)
                    .withRedirect(EG_043 + ACTIVATE_USER)
                    .addToModel(model, config);
        }

        return DONE_EXAMPLE_PAGE;
    }

    @RequestMapping(value = LIST_ENVELOPES, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object listEnvelopes(ModelMap model) throws ApiException {
        String basePath = session.getBasePath();
        String accessToken = user.getAccessToken();

        ApiClient apiClient = createApiClient(basePath, accessToken);
        apiClient.addDefaultHeader(ACT_ON_BEHALF, this.config.getPrincipalUserId());
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);

        EnvelopesInformation envelopesInformation = (new SharedAccessService()).getEnvelopes(
                envelopesApi,
                session.getAccountId());

        if(envelopesInformation.getEnvelopes() == null || envelopesInformation.getEnvelopes().isEmpty()){
            DoneExample.createDefault("No envelopes in the principal user's account")
                    .withMessage(getTextForCodeExampleByApiType().getAdditionalPage().get(2).ResultsPageText)
                    .withRedirect(HOME_PAGE)
                    .addToModel(model, config);
        } else {
            DoneExample.createDefault("Principal's envelopes visible in the agent's Shared Access UI")
                    .withMessage(getTextForCodeExampleByApiType().getAdditionalPage().get(1).ResultsPageText)
                    .withJson(envelopesInformation.toString())
                    .withRedirect(HOME_PAGE)
                    .addToModel(model, config);
        }

        return DONE_EXAMPLE_PAGE;
    }
}
