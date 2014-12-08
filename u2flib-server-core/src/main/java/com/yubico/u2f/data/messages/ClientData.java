package com.yubico.u2f.data.messages;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yubico.u2f.data.messages.key.util.U2fB64Encoding;
import com.yubico.u2f.exceptions.InvalidFacetException;
import com.yubico.u2f.exceptions.U2fException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientData {

    private static final String TYPE_PARAM = "typ";
    private static final String CHALLENGE_PARAM = "challenge";
    private static final String ORIGIN_PARAM = "origin";

    private final String type;
    private final String challenge;
    private final String origin;
    private final String rawClientData;

    public String asJson() {
        return rawClientData;
    }

    public ClientData(String clientData) throws U2fException {
	    System.out.println(clientData);
	    this.rawClientData = new String(U2fB64Encoding.decode(clientData));
	    System.out.println("******"+this.rawClientData);
	    JsonElement clientDataAsElement = new JsonParser().parse(rawClientData);
        if (!clientDataAsElement.isJsonObject()) {
            throw new U2fException("ClientData has wrong format");
        }
        JsonObject jsonObject = clientDataAsElement.getAsJsonObject();
        if (!jsonObject.has(TYPE_PARAM)) {
            throw new U2fException("Bad clientData: missing 'typ' param");
        }
        this.type = jsonObject.get(TYPE_PARAM).getAsString();
        if (!jsonObject.has(CHALLENGE_PARAM)) {
            throw new U2fException("Bad clientData: missing 'challenge' param");
        }
        this.challenge = jsonObject.get(CHALLENGE_PARAM).getAsString();
        this.origin = checkNotNull(jsonObject.get(ORIGIN_PARAM).getAsString());
    }

    @Override
    public String toString() {
        return rawClientData;
    }

    public String getChallenge() {
        return challenge;
    }

    public void checkContent(String type, String challenge, Optional<Set<String>> facets) throws U2fException {
        if (!type.equals(this.type)) {
            throw new U2fException("Bad clientData: bad type " + this.type);
        }
        if (!challenge.equals(this.challenge)) {
            throw new U2fException("Wrong challenge signed in clientData");
        }
        if (facets.isPresent()) {
            verifyOrigin(origin, canonicalizeOrigins(facets.get()));
        }
    }

    private static void verifyOrigin(String origin, Set<String> allowedOrigins) throws InvalidFacetException {
        if (!allowedOrigins.contains(canonicalizeOrigin(origin))) {
            throw new InvalidFacetException(origin +
                    " is not a recognized home origin for this backend");
        }
    }

    public static Set<String> canonicalizeOrigins(Set<String> origins) throws InvalidFacetException {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (String origin : origins) {
            result.add(canonicalizeOrigin(origin));
        }
        return result.build();
    }

    public static String canonicalizeOrigin(String url) throws InvalidFacetException {
        try {
            URI uri = new URI(url);
            if (uri.getAuthority() == null) {
                return url;
            }
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (URISyntaxException e) {
            throw new InvalidFacetException("specified bad origin", e);
        }
    }
}
