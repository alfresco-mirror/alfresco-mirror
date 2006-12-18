/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.web.api;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.util.Base64;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;


/**
 * HTTP Basic Authentication Interceptor
 * 
 * @author davidc
 */
public class BasicAuthentication implements MethodInterceptor
{
    // dependencies
    private AuthenticationService authenticationService;
    
    /**
     * @param authenticationService
     */
    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }
    
    /* (non-Javadoc)
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke(MethodInvocation invocation)
        throws Throwable
    {
        boolean authorized = false;
        Object retVal = null;
        Object[] args = invocation.getArguments();
        APIRequest request = (APIRequest)args[0];
        APIService service = (APIService)invocation.getThis();

        try
        {
            //
            // validate credentials
            // 

            String authorization = request.getHeader("Authorization");
            if ((authorization == null || authorization.length() == 0) && service.getRequiredAuthentication().equals(APIRequest.RequiredAuthentication.Guest))
            {
                // authenticate as guest, if service allows
                authenticationService.authenticateAsGuest();
                authorized = true;
            }
            else if (authorization != null && authorization.length() > 0)
            {
                try
                {
                    String[] authorizationParts = authorization.split(" ");
                    if (!authorizationParts[0].equalsIgnoreCase("basic"))
                    {
                        throw new APIException("Authorization '" + authorizationParts[0] + "' not supported.");
                    }
                    String decodedAuthorisation = new String(Base64.decode(authorizationParts[1]));
                    String[] parts = decodedAuthorisation.split(":");
                    
                    if (parts.length == 1)
                    {
                        // assume a ticket has been passed
                        authenticationService.validate(parts[0]);
                        authorized = true;
                    }
                    else
                    {
                        // assume username and password passed
                        if (parts[0].equals(AuthenticationUtil.getGuestUserName()))
                        {
                            authenticationService.authenticateAsGuest();
                            authorized = true;
                        }
                        else
                        {
                            authenticationService.authenticate(parts[0], parts[1].toCharArray());
                            authorized = true;
                        }
                    }
                }
                catch(AuthenticationException e)
                {
                    // failed authentication
                }
            }

            //
            // execute API service or request authorization
            //
            
            if (authorized)
            {
                retVal = invocation.proceed();
            }
            else
            {
                APIResponse response = (APIResponse)args[1];
                response.setStatus(401);
                response.setHeader("WWW-Authenticate", "Basic realm=\"Alfresco\"");
            }
        }
        finally
        {
            // clear authentication
            // TODO: Consider case where authentication is set before this method is called.
            //       That shouldn't be the case for the web api.
            if (authorized)
            {
                authenticationService.clearCurrentSecurityContext();
            }
        }

        return retVal;
    }

}
