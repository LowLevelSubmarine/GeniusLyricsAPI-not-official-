package genius;

import com.github.scribejava.apis.GeniusApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import core.GLA;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class LyricsCrawler {

    private static final String USER_AGENT = "";
    private static final String GENIUS_SEARCH_URL = "https://api.genius.com/search?q=";

    private OAuth20Service service;
    private String accessToken;
    private GLA gla;

    public LyricsCrawler(String clientId, String accessToken, GLA gla) {

        //Build OAuth20Service
        ServiceBuilder builder = new ServiceBuilder(clientId);
        builder.userAgent(USER_AGENT);
        this.service = builder.build(GeniusApi.instance());

        //Take over access token
        this.accessToken = accessToken;

        //Take over GLA
        this.gla = gla;

    }

    public List<Lyrics> get(String parameter) {
        String cacheId = parameter.toLowerCase();
        if (this.gla.getCrawlCache().containsKey(cacheId)) {
            return this.gla.getCrawlCache().get(cacheId);
        } else {
            List<Lyrics> lyrics =  crawl(parameter);
            this.gla.getCrawlCache().put(cacheId, lyrics);
            return lyrics;
        }
    }

    private List<Lyrics> crawl(String parameter) {
        String url = escapeString(GENIUS_SEARCH_URL + parameter);
        try {
            OAuthRequest request = new OAuthRequest(Verb.GET, url);
            this.service.signRequest(this.accessToken, request);
            Response response = this.service.execute(request);
            return parseResult(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedList<>();
    }

    private List<Lyrics> parseResult(String json) {
        LinkedList<Lyrics> lyrics = new LinkedList<>();
        JSONObject root = new JSONObject(json);
        JSONArray hits = root.getJSONObject("response").getJSONArray("hits");
        for (int index = 0; index < hits.length(); index++) {
            JSONObject hit = hits.getJSONObject(index);
            if (hit.getString("type").equals("song")) {
                JSONObject result = hit.getJSONObject("result");
                if (result.getString("lyrics_state").equals("complete")) {
                    lyrics.add(new Lyrics(result.getString("path"), this.gla));
                }
            }
        }
        return lyrics;
    }

    private String escapeString(String original) {
        return original.replaceAll(" ", "%20");
    }

}
