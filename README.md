# ConfigAPI
This project is a GSON-like project for Bukkit's YML Config API. This project is on maven central! You can add it to your project using the following:

```xml
<dependency>
  <groupId>xyz.mkotb</groupId>
  <artifactId>config-api</artifactId>
  <version>1.1.0</version>
</dependency>
```

If you're not familiar with GSON, let me show you a quick example of what you can do with this API:

```java
public class MyPlugin extends JavaPlugin {
    private MyConfig myConfig;
    private ConfigFactory configFactory = ConfigFactory.newFactory(this);

    @Override
    public void onEnable() {
        myConfig = configFactory.fromFile("config", MyConfig.class);
        getLogger().info(myConfig.getStartMessage());

        if (myConfig.myFavouriteNumbers != null) {
            getLogger().info(myConfig.myFavouriteNumbers.toString());
        }
    }

    @Override
    public void onDisable() {
        // if we changed values in my config while the server is up
        // we should save it
        configFactory.save("config", myConfig);
    }
}

@Comment("This is my header comment")
public class MyConfig {
                    // init values will be saved as part of the "default config"
    private String startMessage = "This awesome bukkit plugin has started!";
    @Comment("Put in your favourite number here!")
    @RequiredField // if this field is not set in the config file due to user error
                   // an exception will be thrown
    private int myFavouriteNumber = 3;
                   // lists, sets, queues, maps, and arrays are supported!
    List<Integer> myFavouriteNumbers;
                   // supports concurrency classes!
    private AtomicInteger myFavouriteAtomicNumber = new AtomicInteger(4);
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

## Interaction with the File

As of currently, if you were to setup and run this plugin, it would write the following to `config.yml` using the default values:

```yml
# This is my header comment
start-message: This awesome bukkit plugin has started!
# Put in your favourite number here!
my-favourite-number: 3
my-favourite-atomic-number: 4
```

As you can see, it only saved the fields which were already set as defaults. If you haven't noticed already, the API changed the naming conventions from Java lower camel case (`likeThis`) to the conventional YAML namespace (`like-this`). You can modify which naming convention the API uses. There will be more information you can refer to on this on [the wiki]( Wiki).

If we were to add a list of our favourite numbers to the config manually, we will be able to see the API being able to load them up without a problem:

```yml
my-favourite-numbers:
- 2
- 6
- 8
```

And if we start up our server with the new config, we'll see the following printed in console:

```
[01:09:42 INFO]: [ConfigTestPlugin] Enabling ConfigTestPlugin v1.0
[01:09:42 INFO]: [ConfigTestPlugin] This super cool awesome bukkit plugin has started!
[01:09:42 INFO]: [ConfigTestPlugin] [2, 6, 8]
```

Well, that was easy.

ConfigAPI is still in early development and will soon be for release for developers to use in their projects. Contributions are more than welcome!
