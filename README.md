<h1 align="center">
  INFUSE
</h1>

<p align="center">Create a new production-ready project with <b>backend</b> (Golang), <b>frontend</b> (JavaScript, TypeScript)<br/>and <b>deploy automation</b> (Ansible, Docker) by running one CLI command.<br/><br/>Focus on <b>writing</b> code and <b>thinking</b> of business-logic! The CLI will take care of the rest.</p>


## INFUSE

INFUSE is a constraint checking engine for context consistency.

### What's new `v2.1` 

- Add support for **MG [ISSRE'22]** for all checking approaches by cli opiton `-mg`. 

### Data Type

#### rawData

```json
{"timestamp": "2011-04-08 04:59:59:152", "fields": {"taxiId": "B214Z0", "longitude": "113.918335", "latitude": "22.5481", "speed": "0", "direction": "270", "status": "0"}}
```
- <font color=red>must contain the timestamp</font>

#### change

```json
{"changeType":"+","patternId":"pat_1","context":{"contextId":"ctx_1","fields":{"key1":"string_value","key2":3.14,"key3":2}}}
```

### How to run

#### normal 


#### testing

```shell
# jdk11
java -jar INFUSE.jar -test -mg -approach INFUSE -rules rules.xml -bfuncs Bfunction.class -contextpool cp.json -incs incsOutput.json -cct cctOutput.txt
```