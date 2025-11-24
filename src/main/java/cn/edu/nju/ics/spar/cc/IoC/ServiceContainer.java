package cn.edu.nju.ics.spar.cc.IoC;

import cn.edu.nju.ics.spar.cc.Util.InfuseException;
import cn.edu.nju.ics.spar.cc.Util.Loggable;
import cn.edu.nju.ics.spar.cc.Services.Impl.LLMServiceImpl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple IoC container to manage engine services and inject them into user classes.
 * Services are registered and injected based on type only.
 */
public class ServiceContainer implements Loggable {
    
    private static final ServiceContainer INSTANCE = new ServiceContainer();
    
    // Map<ServiceType, ServiceInstance> - for type-based injection
    private final Map<Class<?>, Object> services = new HashMap<>();

    private ServiceContainer() {}

    public static ServiceContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Register a service implementation.
     * The service is registered under its own class and all its interfaces.
     * @param service The service instance to register
     */
    public void registerService(Object service) {
        if (service == null) {
            logger.warn("Attempted to register null service");
            return;
        }
        
        Class<?> clazz = service.getClass();
        
        // Register by concrete class
        services.put(clazz, service);
        
        // Register by all interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            services.put(iface, service);
        }
        
        logger.info("Registered service: " + clazz.getName());
    }

    /**
     * Get a service by type.
     * @param serviceType The service type (class or interface)
     * @return The service instance, or null if not found
     */
    public <T> T getService(Class<T> serviceType) {
        Object service = services.get(serviceType);
        if (service == null) {
            return null;
        }
        try {
            return serviceType.cast(service);
        } catch (ClassCastException e) {
            throw new InfuseException("Service type mismatch for: " + serviceType.getName(), e);
        }
    }

    /**
     * Inject dependencies into the target object based on @InfuseResource annotation.
     * @param target The object to inject dependencies into (e.g., Bfunction instance)
     */
    public void inject(Object target) {
        if (target == null) return;

        Class<?> clazz = target.getClass();
        
        // Iterate through all declared fields (including private ones)
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(InfuseResource.class)) {
                Class<?> fieldType = field.getType();
                Object service = services.get(fieldType);

                if (service != null) {
                    try {
                        field.setAccessible(true);
                        field.set(target, service);
                        logger.debug("Injected " + service.getClass().getSimpleName() + 
                                     " into " + clazz.getSimpleName() + "." + field.getName());
                    } catch (IllegalAccessException e) {
                        throw new InfuseException("Failed to inject dependency into field: " + field.getName(), e);
                    }
                } else {
                    logger.warn("No service registered for type: " + fieldType.getName() + 
                                ". Field " + field.getName() + " in " + clazz.getSimpleName() + " will remain null.");
                }
            }
        }
    }
    
    /**
     * Clear all registered services.
     */
    public void clear() {
        services.clear();
    }

    /**
     * Initialize core services based on configuration.
     * This method should be called during application startup.
     */
    public void initializeServices(boolean enableServices) {
        if (enableServices) {
            // Register LLM service when services are enabled
            registerService(new LLMServiceImpl());
        } else {
            logger.info("Services are disabled - skipping service registration");
        }
    }
}
