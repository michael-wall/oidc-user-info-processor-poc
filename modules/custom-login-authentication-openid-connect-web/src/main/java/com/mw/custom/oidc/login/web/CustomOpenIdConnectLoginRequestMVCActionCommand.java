package com.mw.custom.oidc.login.web;

import com.liferay.portal.kernel.exception.UserEmailAddressException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.portlet.url.builder.PortletURLBuilder;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.sso.openid.connect.OpenIdConnectAuthenticationHandler;
import com.liferay.portal.security.sso.openid.connect.OpenIdConnectServiceException;
import com.liferay.portal.security.sso.openid.connect.constants.OpenIdConnectWebKeys;

import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael Wall
 */
@Component(
		immediate = true,
		property = {
			"auth.token.ignore.mvc.action=true",
			"javax.portlet.name=" + PortletKeys.FAST_LOGIN,
			"javax.portlet.name=" + PortletKeys.LOGIN,
			"mvc.command.name=" + OpenIdConnectWebKeys.OPEN_ID_CONNECT_REQUEST_ACTION_NAME,
			"service.ranking:Integer=100"
		},
		service = MVCActionCommand.class
	)
public class CustomOpenIdConnectLoginRequestMVCActionCommand extends BaseMVCActionCommand {
	
	public static final String[] RESERVED_URL_PREFIXES = {"/c/portal/login"};
	
	@Activate
    protected void activate(Map<String, Object> properties) throws Exception {		
		if (_log.isInfoEnabled()) _log.info("Activate...");		
	}

	private boolean isReservedUrlPrefix(String redirectParam) {
		if (Validator.isNull(redirectParam)) return false;
		
		for (int i = 0; i < RESERVED_URL_PREFIXES.length; i++) {
			if (redirectParam.toLowerCase().startsWith(RESERVED_URL_PREFIXES[i].toLowerCase())) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {
		
		_log.info("doProcessAction");

		try {
			HttpServletRequest httpServletRequest =
				_portal.getHttpServletRequest(actionRequest);

			httpServletRequest = _portal.getOriginalServletRequest(
				httpServletRequest);

			HttpServletResponse httpServletResponse =
				_portal.getHttpServletResponse(actionResponse);

			HttpSession httpSession = httpServletRequest.getSession();

			String redirectParam = ParamUtil.getString(actionRequest, "redirect");
		
			_log.info("redirect: " + redirectParam);
			
			if (Validator.isNotNull(redirectParam) && isReservedUrlPrefix(redirectParam)) {
				_log.info("redirect starts with reserved url prefix, not overriding " + OpenIdConnectWebKeys.OPEN_ID_CONNECT_ACTION_URL + " httpSession attribute...");
			} else {
				httpSession.setAttribute(
					OpenIdConnectWebKeys.OPEN_ID_CONNECT_ACTION_URL,
					PortletURLBuilder.createActionURL(
						_portal.getLiferayPortletResponse(actionResponse)
					).setActionName(
						OpenIdConnectWebKeys.OPEN_ID_CONNECT_RESPONSE_ACTION_NAME
					).setRedirect(
						() -> {
							String redirect = ParamUtil.getString(actionRequest, "redirect");
							if (Validator.isNotNull(redirect)) {
								return redirect;
							}

							return null;
						}
					).setParameter(
							"saveLastPath", false
					).buildString()
				);				
			}

			String openIdConnectProviderName = ParamUtil.getString(
				actionRequest,
				OpenIdConnectWebKeys.OPEN_ID_CONNECT_PROVIDER_NAME);

			if (Validator.isNotNull(openIdConnectProviderName)) {
				_openIdConnectAuthenticationHandler.requestAuthentication(
					openIdConnectProviderName, httpServletRequest,
					httpServletResponse);
			}

			long oAuthClientEntryId = ParamUtil.getLong(
				actionRequest, "oAuthClientEntryId");

			if (oAuthClientEntryId > 0) {
				_openIdConnectAuthenticationHandler.requestAuthentication(
					oAuthClientEntryId, httpServletRequest,
					httpServletResponse);
			}
		}
		catch (Exception exception) {
			actionResponse.setRenderParameter(
				"mvcRenderCommandName",
				OpenIdConnectWebKeys.OPEN_ID_CONNECT_REQUEST_ACTION_NAME);

			if (exception instanceof OpenIdConnectServiceException) {
				String message =
					"Unable to communicate with OpenID Connect provider: " +
						exception.getMessage();

				if (_log.isDebugEnabled()) {
					_log.debug(message, exception);
				}

				if (_log.isWarnEnabled()) {
					_log.warn(message);
				}

				SessionErrors.add(actionRequest, exception.getClass());
			}
			else if (exception instanceof
						UserEmailAddressException.MustNotBeDuplicate) {

				if (_log.isDebugEnabled()) {
					_log.debug(exception);
				}

				SessionErrors.add(actionRequest, exception.getClass());
			}
			else {
				_log.error(
					"Unable to process the OpenID Connect login: " +
						exception.getMessage(),
					exception);

				_portal.sendError(exception, actionRequest, actionResponse);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
			CustomOpenIdConnectLoginRequestMVCActionCommand.class);

	@Reference
	private OpenIdConnectAuthenticationHandler
		_openIdConnectAuthenticationHandler;

	@Reference
	private Portal _portal;
	
}