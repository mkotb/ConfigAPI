# ConfigAPI
This project is a GSON-like project for Bukkit's YML Config API. If you're not familiar with GSON, let me show you a quick example of what you can do with this API:

```java
public class MyPlugin extends JavaPlugin {
    private MyConfig myConfig;
    private ConfigFactory configFactory = ConfigFactory.newFactory(this);

    @Override
    public void onEnable() {
        myConfig = configFactory.fromFile("config", MyConfig.class);
        getLogger().info(myConfig.getStartMessage());
    }

    @Override
    public void onDisable() {
        // if we changed values in my config while the server is up
        // we should save it
        configFactory.save("config", myConfig);
    }
}

public class MyConfig {
                    // init values will be saved as part of the "default config"
    private String startMessage = "This awesome bukkit plugin has started!";
    @RequiredField // if this field is not set in the config file due to user error
                   // an exception will be thrown
    private int myFavouriteNumber = 3;
                   // lists, sets, queues, maps, and arrays are supported!
    private List<Integer> myFavouriteNumbers;
                   // supports concurrency classes!
    private int myFavouriteAtomicNumber = new AtomicInteger(4);
                   // myWeapon will be set if it's in the config but won't exist
                   // by default
    private ItemStack myWeapon;
                   // additionally objects within the config are allowed
    private PartOfConfig part;

    public String getStartMessage() {
        return startMessage;
    }
}

public class PartOfConfig {
    private String hi = "my name is billy";
}
```

In the following example, we displayed majority of the features available in the API. The `fromFile()` method takes care of all the initial processes you may need, such as creating the new file and placing default values. Many additional classes such as color, offline players, and vectors are supported to be saved. In the future, extensibility to the Adapter API will be added to where you can use your own data types which can be converted.

ConfigAPI is still in early development and will soon be for release for developers to use in their projects. Contributions are more than welcome!