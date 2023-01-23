package com.docusign;

import com.docusign.common.WorkArguments;
import com.docusign.controller.eSignature.examples.AbstractEsignatureController;
import com.docusign.core.common.Utils;
import com.docusign.core.model.Session;
import com.docusign.core.model.User;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.RecipientViewRequest;
import com.docusign.esign.model.ViewUrl;

import com.docusign.controller.eSignature.services.EmbeddedSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;


/**
 * Use embedded signing.<br/>
 * This example sends an envelope, and then uses embedded signing
 * for the first signer. Embedded signing provides a smoother user experience
 * for the signer: the DocuSign signing is initiated from your site.
 */
@Controller
@RequestMapping("/eg001")
public class EG001ControllerEmbeddedSigning extends AbstractEsignatureController {

    private static final String DOCUMENT_FILE_NAME = "World_Wide_Corp_lorem.pdf";
    private static final String DOCUMENT_NAME = "Lorem Ipsum";
    private static final int ANCHOR_OFFSET_Y = 20;
    private static final int ANCHOR_OFFSET_X = 10;
    private static final String SIGNER_CLIENT_ID = "1000";
    private final Session session;
    private final User user;

    @Autowired
    public EG001ControllerEmbeddedSigning(DSConfiguration config, Session session, User user){
        super(config, Boolean.valueOf(config.getQuickACG()) ? "quickEmbeddedSigning" : "eg001");
        this.session = session;
        this.user = user;
    }

    @Override
    protected void onInitModel(WorkArguments args, ModelMap model) throws Exception {
      if(Utils.isCfr(session.getBasePath(), user.getAccessToken(), session.getAccountId())){
        session.setStatusCFR("enabled");
        throw new Exception(config.getCodeExamplesText().getSupportingTexts().getCFRError());
      }
      if(config.getQuickstart().equals("true")){
        config.setQuickstart("false");
      }
      super.onInitModel(args, model);
    }

    @Override
    protected Object doWork(WorkArguments args, ModelMap model,
                            HttpServletResponse response) throws Exception {
        String signerName = args.getSignerName();
        String signerEmail = args.getSignerEmail();
        String accountId = session.getAccountId();

        // Step 1. Create the envelope definition
        EnvelopeDefinition envelope = EmbeddedSigningService.makeEnvelope(
                signerEmail,
                signerName,
                SIGNER_CLIENT_ID,
                ANCHOR_OFFSET_Y,
                ANCHOR_OFFSET_X,
                DOCUMENT_FILE_NAME,
                DOCUMENT_NAME);

        // Step 2. Call DocuSign to create the envelope
        ApiClient apiClient = createApiClient(session.getBasePath(), user.getAccessToken());

        String envelopeId = EmbeddedSigningService.createEnvelope(apiClient, session.getAccountId(), envelope);
        session.setEnvelopeId(envelopeId);

        // Step 3. create the recipient view, the embedded signing
        RecipientViewRequest viewRequest = EmbeddedSigningService.makeRecipientViewRequest(
                signerEmail,
                signerName,
                SIGNER_CLIENT_ID,
                config.getDsReturnUrl(),
                config.getDsPingUrl());

        ViewUrl viewUrl = EmbeddedSigningService.embeddedSigning(
                apiClient,
                accountId,
                envelopeId,
                viewRequest
        );

        // Step 4. Redirect the user to the embedded signing
        // Don't use an iFrame!
        // State can be stored/recovered using the framework's session or a
        // query parameter on the returnUrl (see the makeRecipientViewRequest method)
        return new RedirectView(viewUrl.getUrl());
    }
}
