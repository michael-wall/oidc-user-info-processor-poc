POC to customise OIDCUserInfoProcessor:

1. _getUserId method updated to try to fetch the user by screenName if the fetch by emailAddress attempt returns null. This will handle use cases where an existing users emailAddress has changed on the oidc IdP and Liferay isn't yet aware of the change. It will NOT handle a use case where both emailAddress AND screenName have changed.
2. processUserInfo method updated to update user details based on the provided claims. The OOTB version doesn't perform any updates if the user already exists.

Local deployment steps:
1. Deploy the custom modules and confirm successful deployment via gogo shell
2. Add the com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl.config config file (located in configs/common) to the environments osgi/config folder
3. Check the status of the OpenIdConnectAuthenticationHandlerImpl OSGi component with this gogo shell command: scr:info com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl

Liferay PaaS setup steps:
1. Add the custom modules to the Liferay service modules folder
2. Add the com.liferay.portal.security.sso.openid.connect.internal.OpenIdConnectAuthenticationHandlerImpl.config config file to the appropriate liferay/configs folder e.g. common to deploy in all environments.
3. Generate a Liferay PaaS build and deploy to the environment.
