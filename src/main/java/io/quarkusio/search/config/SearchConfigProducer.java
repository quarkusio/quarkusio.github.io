package io.quarkusio.search.config;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Unremovable
public class SearchConfigProducer {

    @Inject
    Instance<SearchConfig> configInstance;

    @Produces
    @Named("searchConfig")
    @Unremovable
    public SearchConfig produceSearchConfig() {
        for (Instance.Handle<SearchConfig> handle : configInstance.handles()) {
            if (handle.getBean().getQualifiers().stream().noneMatch(q -> q instanceof Named)) {
                return handle.get();
            }
        }
        throw new IllegalStateException("SearchConfig not found");
    }
}