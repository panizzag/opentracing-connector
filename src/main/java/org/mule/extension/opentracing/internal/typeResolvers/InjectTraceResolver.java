package org.mule.extension.opentracing.internal.typeResolvers;

import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Map;

public class InjectTraceResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String> {

    @Override
    public String getCategoryName() {
        // TODO Auto-generated method stub
        return "InjectTrace";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, String key)
            throws MetadataResolvingException, ConnectionException {
        return context.getTypeLoader().load(Map.class);
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String key)
            throws MetadataResolvingException, ConnectionException {

        return context.getTypeBuilder().anyType().build();
    }

    @Override
    public String getResolverName() {
        return "InjectTraceResolver";
    }

}
