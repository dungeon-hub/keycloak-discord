package net.dungeonhub.keycloak.broker.oidc.mappers;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;

public class AvatarMapper extends AbstractClaimMapper
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
        return "Avatar Importer";
    }

    @Override
    public String getDisplayType()
    {
        return "Discord Avatar Importer";
    }

    @Override
    public String getHelpText()
    {
        return "If possible, import the avatar into the attribute \"picture\".";
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

        this.syncAvatar(realm, user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        this.syncAvatar(realm, user, mapperModel, context);
    }

    private void syncAvatar(RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        JsonNode profileJsonNode = (JsonNode) context.getContextData().get(OIDCIdentityProvider.USER_INFO);

        String id = profileJsonNode.get("id").asText();

        String avatar = profileJsonNode.get("avatar").asText();
        String picture = "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ((avatar != null && avatar.startsWith("a_")) ? ".gif" : ".png");

        if (avatar != null)
        {
            user.setSingleAttribute("picture", picture);
        }
    }
}