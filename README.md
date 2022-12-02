<h1 align="center">
  INFUSE
</h1>

<p align="center">A <b>general constraint checking enginel</b> for data consistency.</p>

## 🚩 Tutorial

First, download **INFUSE_v2.1.jar** into your work directory.

Next, write your own rules and patterns in **rules.xml** and **patterns.xml** respectively according to their [templates](#templates).



## :page_facing_up: <span id="templates">Templates</span>

### Rule template

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

### Pattern template

Patterns (e.g., pat_room1 and pat_room2 in rule template) are used in `forall` and `exists` formulas to show what kind of context the rule is interested in.

Each pattern requires **freshness** and **matcher** that specify **how long** a context stays in this pattern and **which** context can be added into it, respectively.

- **freshness** consists of a **type** and a **value**, where the type can be `time` (ms) or `number` (#). 
- **matcher** 

```XML
<!-- patterns.xml -->
<?xml version="1.0"?>

<patterns>

    <pattern>
        <id>pat_1</id> <!-- unique id -->
        <freshness> <!-- how long a context stays in this pattern -->
            <type>number</type> <!-- type can be time or number -->
            <value>10</value>
        </freshness>
        <matcher> <!-- 可选项, 缺省了的话，所有的数据都会自动匹配 -->
            <type>function</type>
            <functionName>filter</functionName>
            <extraArgumentList> <!-- 可选项 --><!-- 由用户自定义的匹配函数除context之外的额外参数 -->
                <argument>argOne</argument>
                <argument>argTwo</argument>
            </extraArgumentList>
        </matcher>
    </pattern>

    <!-- 一个pattern对应多个sensor, 应该保证多个sensor的field是相同的 -->
    <pattern>
        <id>pat_template2</id>
        <freshness>
            <type>time</type> <!--可选项有time(ms)和number(#)-->
            <value>2000</value> <!--2000ms-->
        </freshness>
        <matcher> <!-- 可选项 -->
            <type>primaryKey</type>
            <primaryKey>id</primaryKey>
            <optionalValueList>
                <value>orange_car</value>
                <value>black_car</value>
            </optionalValueList>
        </matcher>
    </pattern>

</patterns>


```
