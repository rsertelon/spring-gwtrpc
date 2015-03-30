package fr.sertelon.spring.gwtrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

@SuppressWarnings("serial")
public class SpringRemoteServiceServlet extends RemoteServiceServlet {

    private static final String X_GWT_PATH_PREFIX_HEADER = "X-GWT-Path-Prefix";

    @Override
    public String processCall(String payload) throws SerializationException {
        // First, check for possible XSRF situation
        checkPermutationStrongName();

        RPCRequest rpcRequest = null;
        try {
            rpcRequest = RPC.decodeRequest(payload, null, this);
            onAfterRequestDeserialized(rpcRequest);

            return RPC.invokeAndEncodeResponse(getService(payload), rpcRequest.getMethod(), rpcRequest.getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
        } catch (IncompatibleRemoteServiceException ex) {
            log("An IncompatibleRemoteServiceException was thrown while processing this call.", ex);
            return RPC.encodeResponseForFailure(null, ex);
        } catch (RpcTokenException tokenException) {
            log("An RpcTokenException was thrown while processing this call.", tokenException);
            return RPC.encodeResponseForFailedRequest(rpcRequest, tokenException);
        }
    }

    private Object getService(String payload) throws BeansException, SerializationException {
        try {
            ServerSerializationStreamReader streamReader = new ServerSerializationStreamReader(Thread.currentThread().getContextClassLoader(), this);
            streamReader.prepareToRead(payload);
            String interfaceName = streamReader.readString();

            return BeanFactoryUtils.beanOfTypeIncludingAncestors(WebApplicationContextUtils.getWebApplicationContext(getServletContext()), Class.forName(interfaceName));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {
        String gwtPathHeader = request.getHeader(X_GWT_PATH_PREFIX_HEADER);

        if (gwtPathHeader != null) {
            try {
                URL moduleBaseAsURL = new URL(moduleBaseURL);
                String originalPath = moduleBaseAsURL.getPath();
                String modifiedPath = originalPath.substring(gwtPathHeader.length());

                if (originalPath.startsWith(gwtPathHeader) && modifiedPath.startsWith("/")) {
                    moduleBaseURL = moduleBaseURL.replaceFirst(Pattern.quote(originalPath), modifiedPath);
                } else {
                    StringBuilder sb = new StringBuilder("There might be an error in your Reverse Proxy configuration. Got ") //
                            .append(X_GWT_PATH_PREFIX_HEADER) //
                            .append("=") //
                            .append(gwtPathHeader) //
                            .append(" but moduleBaseURL's path is ") //
                            .append(originalPath);

                    log(sb.toString());
                }
            } catch (MalformedURLException e) {
                log("moduleBaseURL is not a URL!", e);
            }
        }

        return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
    }
}
