package demo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.AuthenticateResponse;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.data.messages.RegisterResponse;
import com.yubico.u2f.exceptions.U2fException;
import demo.view.AuthenticationView;
import demo.view.RegistrationView;
import io.dropwizard.views.View;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class Resource {

    public static final String SERVER_ADDRESS = "http://example2.com:8080";
    public static final String NAVIGATION_MENU = "<h2>Navigation</h2><ul><li><a href='registerIndex'>Register</a></li><li><a href='loginIndex'>Login</a></li></ul>.";

    private final Map<String, String> requestStorage = new HashMap<String, String>();
    private final Multimap<String, String> userStorage = ArrayListMultimap.create();
    private final U2F u2f = new U2F();

    private Iterable<DeviceRegistration> getRegistrations(String username) {
        Collection<String> serializedRegistrations = userStorage.get(username);
        List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
        for(String serialized : serializedRegistrations) {
            registrations.add(DeviceRegistration.fromJson(serialized));
        }
        return registrations;
    }

    private void addRegistration(String username, DeviceRegistration registration) {
        userStorage.put(username, registration.toJson());
    }

    @Path("startRegistration")
    @GET
    public View startRegistration(@QueryParam("username") String username) {
        RegisterRequestData registerRequestData = u2f.startRegistration(SERVER_ADDRESS, getRegistrations(username));
        requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());
        return new RegistrationView(registerRequestData.toJson(), username);
    }

    @Path("finishRegistration")
    @POST
    public String finishRegistration(@FormParam("tokenResponse") String response, @FormParam("username") String username)
            throws U2fException {
        RegisterResponse registerResponse = RegisterResponse.fromJson(response);
	    System.out.println(response);
	    System.out.println(requestStorage.get(registerResponse.getRequestId()));
	    RegisterRequestData registerRequestData = RegisterRequestData.fromJson(requestStorage.get(registerResponse.getRequestId()));
        DeviceRegistration registration = u2f.finishRegistration(registerRequestData, registerResponse);
        addRegistration(username, registration);
        requestStorage.remove(registerResponse.getRequestId());
        return "<p>Successfully registered device:</p><pre>" +
                registration +
                "</pre>" + NAVIGATION_MENU;
    }

    @Path("startAuthentication")
    @GET
    public View startAuthentication(@QueryParam("username") String username) throws U2fException {
        AuthenticateRequestData authenticateRequestData = u2f.startAuthentication(SERVER_ADDRESS, getRegistrations(username));
        requestStorage.put(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
        return new AuthenticationView(authenticateRequestData.toJson(), username);
    }

    @Path("finishAuthentication")
    @POST
    public String finishAuthentication(@FormParam("tokenResponse") String response,
                                       @FormParam("username") String username) throws U2fException {
        AuthenticateResponse authenticateResponse = AuthenticateResponse.fromJson(response);
        AuthenticateRequestData authenticateRequest = AuthenticateRequestData.fromJson(requestStorage.get(authenticateResponse.getRequestId()));
        requestStorage.remove(authenticateResponse.getRequestId());
        u2f.finishAuthentication(authenticateRequest, authenticateResponse, getRegistrations(username));
        return "<p>Successfully authenticated!<p>" + NAVIGATION_MENU;
    }

    @Path("loginIndex")
    @GET
    public String loginIndex() throws Exception {
        /*URL index = Resource.class.getResource("loginIndex.html");
        return Files.toString(new File(index.toURI()), Charsets.UTF_8);*/
	    InputStream a = Resource.class.getResourceAsStream("loginIndex.html");
	    return getStringFromInputStream(a);
    }

    @Path("registerIndex")
    @GET
    public String registerIndex() throws Exception {
        /*URL defaultImage = Resource.class.getResource("registerIndex.html");
	    System.out.println(defaultImage.toURI().toString());
	    return Files.toString(new File(defaultImage.toURI()), Charsets.UTF_8);*/
	    InputStream a = Resource.class.getResourceAsStream("registerIndex.html");
	    return getStringFromInputStream(a);
    }

	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}
}
