package org.rapla.plugin.urlencryption;

import org.jetbrains.annotations.PropertyKey;
import org.rapla.components.i18n.AbstractBundle;
import org.rapla.components.i18n.BundleManager;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.inject.Extension;

import javax.inject.Inject;

@Extension(provides = I18nBundle.class, id = UrlEncryptionResources.PLUGIN_ID)
public class UrlEncryptionResources extends AbstractBundle
{
        public static final String PLUGIN_ID ="org.rapla.plugin.urlencryption";
        private static final String BUNDLENAME = PLUGIN_ID +  ".UrlEncryptionResources";
        @Inject
        public UrlEncryptionResources(BundleManager loader)
        {
            super(BUNDLENAME, loader);
        }
        public String getString(@PropertyKey(resourceBundle = BUNDLENAME) String key)
        {
            return super.getString(key);
        }

        public String format(@PropertyKey(resourceBundle = BUNDLENAME) String key, Object... obj)
        {
            return super.format(key, obj);
        }

}
