package net.dungeonhub.keycloak.broker.oidc.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vdurmont.emoji.EmojiParser;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.social.discord.DiscordIdentityProviderFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuildsMapper extends AbstractClaimMapper
{
    private static final Logger logger = Logger.getLogger(GuildsMapper.class);

    private static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID,
            DiscordIdentityProviderFactory.PROVIDER_ID
    };
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuildsMapper.class);

    @Override
    public String[] getCompatibleProviders()
    {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory()
    {
        return "Guild Importer";
    }

    @Override
    public String getDisplayType()
    {
        return "Discord Guild Importer";
    }

    @Override
    public String getHelpText()
    {
        return "If possible, import the users guilds into a field.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of();
    }

    @Override
    public String getId()
    {
        return "oidc-guilds-idp-mapper";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        super.importNewUser(session, realm, user, mapperModel, context);

        this.syncGuilds(realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        this.syncGuilds(realm, user, mapperModel, context);
    }

    private void syncGuilds(RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        JsonNode profileJsonNode = (JsonNode) context.getContextData().get(OIDCIdentityProvider.USER_INFO);
        JsonNode guilds = profileJsonNode.get("discord-guilds");

        List<String> attributes = new ArrayList<>();

        for (Iterator<JsonNode> it = guilds.elements(); it.hasNext(); )
        {
            JsonNode subNode = it.next();

            ObjectNode guildObject = JsonNodeFactory.instance.objectNode();
            guildObject.set("id", subNode.get("id"));
            guildObject.set("icon", subNode.get("icon"));
            guildObject.set("name", JsonNodeFactory.instance.textNode(EmojiParser.parseToAliases(subNode.get("name").asText())));

            attributes.add(guildObject.toString());
        }

        user.setAttribute("discord-guild", attributes);

        List<String> permissions = user.getAttributes().getOrDefault("permission", new ArrayList<>());
        for (Iterator<JsonNode> it = guilds.elements(); it.hasNext(); )
        {
            JsonNode subNode = it.next();

            if (subNode.get("owner").asBoolean() && !permissions.contains("server_" + subNode.get("id").asText()))
            {
                permissions.add("server_" + subNode.get("id").asText());
            }
        }


        user.setAttribute("permission", permissions);
    }
}