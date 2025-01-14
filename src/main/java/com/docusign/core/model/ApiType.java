package com.docusign.core.model;

import java.util.Arrays;

public enum ApiType {
    ESIGNATURE("eSignature API", new String[]{"signature"}, "eg"),
    ROOMS("Rooms API", new String[]{"signature", "dtr.rooms.read", "dtr.rooms.write", "dtr.documents.read", "dtr.documents.write", "dtr.profile.read", "dtr.profile.write", "dtr.company.read", "dtr.company.write", "room_forms"}, "r"),
    CONNECT("Connect", new String[] {}, "con"),
    CLICK("Click API", new String[] {"click.manage", "click.send"}, "c"),
    MONITOR("Monitor API", new String[] {"signature", "impersonation"}, "m"),
    ADMIN("Admin API",  new String[] {"user_write", "signature", "impersonation", "group_read", "organization_read", "permission_read", "user_read", "account_read", "domain_read", "identity_provider_read", "user_data_redact", "asset_group_account_read", "asset_group_account_clone_write", "asset_group_account_clone_read"}, "a");

    final String value;

    final String[] scopes;

    final String codeName;

    ApiType(String value, String[] scopes, String codeName) {
        this.value = value;
        this.scopes = scopes;
        this.codeName = codeName;
    }

    public static ApiType giveTypeByName(String exampleName) {
        return Arrays.stream(ApiType.values())
            .filter(x -> exampleName.contains(x.getCodeName()))
            .findFirst()
            .get();
    }

    public String[] getScopes() {
        return scopes;
    }

    public String getCodeName() {
        return codeName;
    }
}