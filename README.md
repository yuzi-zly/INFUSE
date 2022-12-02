<h1 align="center">
  INFUSE
</h1>

<p align="center">A <b>general constraint checking enginel</b> for data consistency.</p>

## 🚩 Tutorial

1. Download **INFUSE_v2.1.jar** into your work directory.

2. Write your own rules and patterns in **rules.xml** and **patterns.xml** according to their [templates](#templates).

3. Write your own **bfunctions** and **mfunctions** ([Dynamic loaded functions](#dynamic)) in **java** and compile them into **class**.

4. Convert your data into [standard data formats]().

5. run [cli command]() to start checking. 

## :page_facing_up: <span id="templates">Templates</span>

### Rule Template

Rules are writtern in **first-order logic** style language which contains **seven** formula types. 

For example, a physical law  "*no one can be in two rooms at the same time*" can be writtern as:

```XML
<!-- rules.xml -->
<?xml version="1.0"?>

<rules>

    <rule>
        <id>rule_02</id> <!-- unique id -->
        <formula>
            <forall var="v1" in="pat_room1">
                  <not>
                    <exists var="v2" in="pat_room2">
                      <bfunc name="samePerson">
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

### Pattern Template

Patterns (e.g., pat_room1 and pat_room2 in rule template) are used in `forall` and `exists` formulas to show what kind of context the rule is interested in.

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
  - `function` matcher requires a `functionName` and an `extraArumentList` (please read [mfunction](#mfunc) part for more information)
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

For example, pattern `pat_car` only interests in red cars with 500ms freshness can be writtern as:

```XML
<!-- patterns.xml -->
<?xml version="1.0"?>

<patterns>

    <pattern>
        <id>pat_car</id> <!-- unique id -->
        <freshness> <!-- how long a context stays in this pattern -->
            <type>time</type> 
            <value>500</value>
        </freshness>
        <matcher> <!-- which context can be added into this pattern -->
              <type>primaryKey</type>
              <primaryKey>car_color</primaryKey>
              <optionalValueList> 
                <value>red</value>
              </optionalValueList>
        </matcher>
    </pattern>

</patterns>
```

## 🚀 <span id="dynamic">Dynamic Loaded Functions</span>

### <span id="bfunc">Bfunction</bfunc>


### <span id="mfunc">Mfunction</bfunc>

## ⚙ Commands and Options

