# nf-co2footprint plugin 
 
This Nextlow plugins implements the calculation of the CO2 footprint of running a workflow and each of its independent tasks.

## Plugin classes

- `CO2FootprintFactory`: Implements `TraceObserverFactory`. Implements the validation observer factory
- `CO2FootprintTextFileObserver`: Implements `TraceObserver`. Creates a trace file with CO2 footprint data.
- `CO2FootprintConfig`: Defines the path of output files containing CO2 footprint results and the summary.
- `CO2FootprintPlugin`: Implements the plugin entry point.
- `CO2FootprintFactoryTest`: Tests for class `CO2FootprintFactory`.

## Unit testing 

Run the following command in the project root directory (ie. where the file `settings.gradle` is located):

```bash
./gradlew check
```

## Testing and debugging

To run and test the plugin in for development purpose, configure a local Nextflow build with the following steps:

1. Clone the Nextflow repository in your computer into a sibling directory:
    ```bash
    git clone --depth 1 https://github.com/nextflow-io/nextflow ../nextflow
    ```
  
2. Configure the plugin build to use the local Nextflow code:
    ```bash
    echo "includeBuild('../nextflow')" >> settings.gradle
    ```
  
   (Make sure to not add it more than once!)

3. Compile the plugin alongside the Nextflow code:
    ```bash
    make compile
    ```

4. Run Nextflow with the plugin, using `./launch.sh` as a drop-in replacement for the `nextflow` command, and adding the option `-plugins nf-hello` to load the plugin:
    ```bash
    ./launch.sh run nextflow-io/hello -plugins nf-hello
    ```

## Testing without Nextflow build

The plugin can be tested without using a local Nextflow build using those steps:

1. generate required artifacts with `make buildPlugins`
2. copy build/plugins/your-plugin to `$HOME/.nextflow/plugins`
3. create a pipeline with your plugin and see in action via `nextflow run ./my-pipeline-script.nf`

## Package, upload and publish

The project should be hosted in a GitHub repository whose name should match the name of the plugin, that is the name of the directory in the `plugins` folder (e.g. `nf-hello`).

Follow these steps to package, upload and publish the plugin:

1. Create a file named `gradle.properties` in the project root containing the following attributes (this file should not be committed to Git):

   * `github_organization`: the GitHub organisation where the plugin repository is hosted.
   * `github_username`: The GitHub username granting access to the plugin repository.
   * `github_access_token`: The GitHub access token required to upload and commit changes to the plugin repository.
   * `github_commit_email`: The email address associated with your GitHub account.

2. Use the following command to package and create a release for your plugin on GitHub:
    ```bash
    ./gradlew :plugins:nf-hello:upload
    ```

3. Create a pull request against [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) to make the plugin accessible to Nextflow.
