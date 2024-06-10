**POC to customise OOTB behaviour of OIDCUserInfoProcessor**

**Summary**
1. _getUserId method updated to try to fetch the user by screenName if the fetch by emailAddress attempt returns null. This will handle use cases where an existing users emailAddress has changed on the oidc IdP and Liferay isn't yet aware of the change. It will NOT handle a use case where both emailAddress AND screenName have changed.
2. The OOTB version doesn't perform any updates if the user already exists. processUserInfo method has been updated to update Liferay user details based on the provided claims, but only if an update is necessary. The claims that are checked before updating are:
- emailAddress
- screenName
- firstName
- lastName
- middleName

**Liferay Version:**
- This POC is based on Liferay DXP 7.4 U92 source code. (i.e. OIDCUserInfoProcessor.java).
- Ensure the OOTB code in CustomOIDCUserInfoProcessor is checked when upgrading Liferay to ensure changes to the OOTB OIDCUserInfoProcessor aren't missed.
  
**Custom OSGi modules:**
- custom-oidc-user-processor contains CustomOIDCUserInfoProcessor to replace the OOTB OIDCUserInfoProcessor class with the custom logic. Due to the OIDCUserInfoProcessor setup, the class extends OIDCUserInfoProcessor with OSGi Component service set to OIDCUserInfoProcessor, but it duplicates all methods from OIDCUserInfoProcessor (as all except processUserInfo are private). Existing methods processUserInfo and _getUserId have been updated, and method _updateUser has been added. All other methods are unchanged from OIDCUserInfoProcessor.
- portal-security-sso-openid-connect-fragment: Fragment module to export internal packages from com.liferay.portal.security.sso.openid.connect.impl so they can be used in custom-oidc-user-processor.

**OSGi config:**
- com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl.config config file to inject CustomOIDCUserInfoProcessor into _oidcUserInfoProcessor in OpenIdConnectAuthenticationHandlerImpl class.

**Local setup and deployment steps:**
1. Deploy the custom modules and confirm successful deployment via gogo shell
2. Add the com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl.config config file (located in configs/common) to the environments osgi/config folder

**Liferay PaaS setup steps:**
1. Add the custom modules to the Liferay service modules folder
2. Add the com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl.config config file to the appropriate liferay/configs folder e.g. common to deploy in all environments.
3. Generate a Liferay PaaS build and deploy to the environment.

**Validate Deployment (for local and Liferay PaaS):**
1. Check the state of the OSGi modules with the lb command:
- custom-oidc-user-processor should have state Active.
- portal-security-sso-openid-connect-fragment should have state Resolved (as it is a fragment module)
2. The Liferay logs should have the following logging to show the fragment module has attached itself to the com.liferay.portal.security.sso.openid.connect.impl module as expected:
- 2024-06-10 11:38:23.326 INFO  [Refresh Thread: Equinox Container: 3e29d73a-32d3-4c24-82cb-cd239d8e3a4f][BundleStartStopLogger:71] STOPPED com.liferay.portal.security.sso.openid.connect.impl_7.0.27 [848]
- 2024-06-10 11:38:23.585 INFO  [Refresh Thread: Equinox Container: 3e29d73a-32d3-4c24-82cb-cd239d8e3a4f][BundleStartStopLogger:68] STARTED com.liferay.portal.security.sso.openid.connect.impl_7.0.27 [848]
3. Check the status of the OpenIdConnectAuthenticationHandlerImpl OSGi component with these Gogo shell commands:
4. Run command 'scr:info com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl' and ensure all of the References are satisfied in the output, in particular the following:
**_oidcUserInfoProcessor: com.liferay.portal.security.sso.openid.connect.internal.OIDCUserInfoProcessor SATISFIED 1..1 static**
5. Run command 'lb com.liferay.portal.security.sso.openid.connect.impl' and using the bundle id from the output run command 'b [bundle_id]' e.g. 'b 848' and within the output 'Services in use' section check for the following to show that CustomOIDCUserInfoProcessor is being used in place of OIDCUserInfoProcessor:
{com.liferay.portal.security.sso.openid.connect.internal.OIDCUserInfoProcessor}={component.id=xxxxx, **component.name=com.mw.custom.oidc.CustomOIDCUserInfoProcessor**, service.id=xxxxx, service.scope=bundle, service.bundleid=xxxxx}
