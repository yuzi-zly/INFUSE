<h1 align="center">
  INFUSE
</h1>

<p align="center">A <b>general constraint checking engine</b> for data consistency.<br/><br/>For more technique details, please refer to <a href="#papers">papers</a>.</p>

## :round_pushpin: Start

1. Download [INFUSE_v1.0_stable_jdk11.jar](https://github.com/yuzi-zly/INFUSE/releases/tag/v1.0_stable) into your work directory.

2. Write your own rules and patterns in [rules.xml](#rule) and [patterns.xml](#pattern).

3. Write your own [bfunctions](#bfunc) and [mfunctions](#mfunc) in **java** and compile them into **class** files (e.g., Bfunction.class and Mfunction.class).

4. Convert your data into [standard data formats](#dataFormat).

5. Run INFUSE with [options](#options) to start checking. 

## :scroll: Templates

### <span id="rule"> Rule Template </span>

Rules are writtern in **first-order logic** style language which contains **seven** formula types. 

For example, a requirement  "*Subways on the same line should be separated from each other by a certain distance*" can be writtern as:

```XML
<!-- rules.xml -->
<?xml version="1.0"?>

<rules>

    <rule>
        <id>rule_02</id> <!-- unique id -->
        <formula>
            <forall var="v1" in="pat_metro1">
                  <not>
                    <exists var="v2" in="pat_metro2">
                      <bfunc name="dangerousDistance">
                          <param pos="1" var="v1"/>
                          <param pos="2" var="v2"/>
                      </bfunc>
                    </exists>
                  </not>
            </forall>
        </formula>
    </rule>

</rules>
```

Syntax for `and`, `or` and `implies` formula is as follows:

```XML
<and> <!-- can be or, implies -->
  <formula1> ... </formula1> <!-- formula1 can be all seven types -->
  <formula2> ... </formula2> <!-- formula1 can be all seven types -->
</and>
```
> :bell: Please make sure that every [pattern](#pattern) (e.g., pat_metro1) is **only used once** in the rule file, otherwise some errors may occur while using specific options like `-approach PCC+IMD`.

### <span id="pattern"> Pattern Template </span>

Patterns (e.g., pat_metro1 and pat_metro2 in rule template) are used in `forall` and `exists` formulas to show what kind of context the rule is interested in.

Each pattern requires a `freshness` and a `matcher` that specify **how long a context stays in this pattern** and **which context can be added into it**, respectively.

- `freshness` consists of a `type` and a `value`, where the type can be `time` (ms) or `number` (#). 

```XML
<!-- freshness structures -->

<!-- time freshness -->
<freshness> 
    <type>time</type> 
    <value>1000</value>
</freshness>

<!-- number freshness -->
<freshness> 
    <type>number</type> 
    <value>10</value>
</freshness>
```

- `matcher` is an **optional** component. If it is not explicitly writtern, **every context matches this pattern**, otherwise there are two explicit matcher types `function` and `primaryKey`.
  - `function` matcher requires a `functionName` and an `extraArumentList`[optional] (please read [mfunction](#mfunc) part for more information)
  - `primaryKey` matcher requires a `primaryKey` and an `optionalValueList`

```XML
<!-- matcher structures -->

<!-- function matcher -->
<matcher> 
    <type>function</type>
    <functionName>filter</functionName>
    <extraArgumentList> <!-- extra parameters for the filter function other than contexts -->
        <argument>arg1</argument>
        <argument>arg2</argument>
    </extraArgumentList>
</matcher>

<!-- primaryKey matcher -->
<matcher> 
    <type>primaryKey</type>
    <primaryKey>my_primaryKey</primaryKey>
    <optionalValueList> <!-- optional values for my_primaryKey -->
      <value>validValue1</value>
      <value>validValue2</value>
    </optionalValueList>
</matcher>
```

For example, pattern `pat_metro1` only interests in the latest record of running subways can be writtern as:

```XML
<!-- patterns.xml -->
<?xml version="1.0"?>

<patterns>

    <pattern>
        <id>pat_metro1</id> <!-- unique id -->
        <freshness> <!-- how long a context stays in this pattern -->
            <type>number</type> 
            <value>1</value>
        </freshness>
        <matcher> <!-- which context can be added into this pattern -->
              <type>primaryKey</type>
              <primaryKey>metro_status</primaryKey>
              <optionalValueList> 
                <value>running</value>
              </optionalValueList>
        </matcher>
    </pattern>

</patterns>
```

## :hotsprings: Dynamic Loaded Functions

### <span id="bfunc">Bfunction</bfunc>

Bfunction is the terminal of [rules](#rule). It is a **domain-specific** fucntion that takes values of variables (i.e., contexts) as input and return a Boolean value (True or False).

INFUSE requires a java class file containing specific semantics of every `bfunc` in rules. 

To get such a java class file, firstly write a java file with following skeleton:

```java
//Bfunction.java
import java.util.Map;
public class Bfunction {
    //entry
    public boolean bfunc(String funcName,
      //a context is modeled as a Map that maps fields to values
      Map<String, Map<String, String>> var2ctxs 
      ) throws Exception {
        switch (funcName){
            case "bfunc1": return func_bfunc1(var2ctxs);
            case "bfunc2": return func_bfunc2(var2ctxs);
            // as more as you want
            default: throw new Exception();
        }
    }

    //specific semantics
    private boolean func_bfunc1(Map<String, Map<String, String>> var2ctxs){
      //your codes
    }

    private boolean func_bfunc2(Map<String, Map<String, String>> var2ctxs){
      //your codes
    }
}
```

> :bell: Make sure the signature of entry is exactly the same as that in skeleton. 

Then, compile the java file to class file.

> :bell: Use the same java version for compiling the java file and run the INFUSE engine.

### <span id="mfunc">Mfunction</bfunc>

Mfunction is used in `function` matcher of [patterns](#pattern). It enables users to customize the way context and pattern match.

INFUSE also requires a java class file containing specific semantics of every `mfunc` in patterns.

To get such a java class file, firstly write a java file with following skeleton:

```java
//Mfunction.java
import java.util.List;
import java.util.Map;
public class Mfunction 
    //entry
    public boolean mfunc(final String funcName,
      final Map<String, String> ctxFields, //a context is modeled as a Map
      final List<String> extraArgumentList //corresponding to the extraArgumentList in pattern
      ) throws Exception {
        switch (funcName){
            case "mfunc1": return func_mfunc1(ctxFields, extraArgumentList);
            case "mfunc2": return func_mfunc2(ctxFields, extraArgumentList);
            //as more as you want
            default: throw new Exception();
        }
    }

    private boolean func_mfunc1(final Map<String, String> ctxFields,
      final List<String> extraArgumentList){
        //your codes
    }

    private boolean func_mfunc2(final Map<String, String> ctxFields,
      final List<String> extraArgumentList){
        //your codes
    }
}
```

> :bell: Make sure the signature of entry is exactly the same as that in skeleton. 

Then, compile the java file to class file.

> :bell: Use the same java version for compiling the java file and run the INFUSE engine.

## :paperclip: <span id="dataFormat">Data Formats</span>

### Input Data

#### Raw Data

One piece of input data follows **json** format. It consists of `timestamp` and `fields`, where the timestamp follows `yyyy-MM-dd HH:mm:ss:SSS` format and the fields contain data values with different keys.

```json
{"timestamp": "2011-04-08 04:00:00:000", "fields" : {"key1": "value1", ...}}
```

#### Context Changes

INFUSE also supports change-type input data, the format of which is as follows.

```json
{ "changeType": "+", "patternId": "pat_1", "context": { "contextId": "ctx_1", "fields": { "key1": "value1",...}}}
```

### Output Data

INFUSE would output all detected inconsistencies in a TXT file, in which one line represents one inconsistency. The format of inconsistency is as follows:

```txt
[ruleId]([VIOLATED/SATISFIED],{(variable, contextId),...})
```

For example, if rule "rule_02" is violated (evaluted to be false) when "v1" and "v2" are assigned context "ctx_2" and context "ctx_3" respectively, the corresponding inconsistency is

```txt
rule_02(VIOLATED,{(v1,2),(v2,3)})
```

## :rocket: <span id="options">Options</span>

|Option|Description|Type|Available candidates| 
|:---:|:---:|:---:|:---:|
|`-help`|Print the usage|`bool`|None|
|`-mode`|Run under the given mode|`argument`|`offline`, `online`|
|`-approach`|Use the specified approach for checking|`argument`|`ECC+IMD`,`ECC+GEAS_ori`,`PCC+IMD`,`PCC+GEAS_ori`,`ConC+IMD`,`ConC+GEAS_ori`,`INFUSE`|
|`-rules`|Load rules from given file (XML file)|`argument`|None|
|`-bfuncs`|Load bfunctions from given file (Class file)|`argument`|None|
|`-patterns`|Load patterns from given file (XML file)|`argument`|None|
|`-mfuncs`|Load mfunctions from given file (Class file)|`argument`|None|
|`-mg`|Enable link generation minimization|`bool`|None|
|`-incs`|Write detected inconsistencies to given file|`argument`|None|
|`-data`|Read data from given file (only under `offline` mode)|`argument`|None|
|`-dataType`|Specify the type of data in dataFile|`argument`|`rawData`,`change`|

> :bell: Option `-data` only can be used under `offline` mode. 

> :bell: INFUSE would build a UDP socket (localhost:6244) for receiving data under `online` mode.

For example, if we want use `INFUSE` approach to check the consistency of data in **data.txt** with rules in **rules.xml**, patterns in **patterns.xml**, bfunctions in **Bfunction.class**, and mfunctions in **Mfunction.class** under `offline` mode with `MG`, we can use the following commands and detected inconsistencies would be output in **incs.txt**.

```shell
java -jar INFUSE_v1.0_stable_jdk11.jar -mode offline -approach INFUSE -data data.txt -dataType rawData -rules rules.xml -patterns patterns.xml -bfuncs Bfunction.class -mfuncs Mfunction.class -mg -incs incs.txt 
```

## :bookmark_tabs: <span id="papers">Main Papers</span>

- [ISSRE'22] [Minimizing Link Generation in Constraint Checking for Context Inconsistency Detection](https://lyzhang.site/publications/ISSRE22.pdf)
- [ICSME'22] [INFUSE: Towards Efficient Context Consistency by Incremental-Concurrent Check Fusion](https://lyzhang.site/publications/ICSME22.pdf)
- [TSE'21] [Generic Adaptive Scheduling for Efficient Context Inconsistency Detection](http://www.why.ink:8080/static/publications/TSE_2020.pdf)
- [ICSME'17] [GEAS: Generic Adaptive Scheduling for High-efficiency Context Inconsistency Detection](https://cs.nju.edu.cn/changxu/1_publications/17/ICSME17.pdf)
- [SCIS'13] [Towards Context Consistency by Concurrent Checking for Internetware Applications. Science](https://cs.nju.edu.cn/changxu/1_publications/13/SCIS13.pdf)
- [TOSEM'10] [Partial Constraint Checking for Context Consistency in Pervasive Computing](https://cs.nju.edu.cn/changxu/1_publications/10/TOSEM10.pdf)
