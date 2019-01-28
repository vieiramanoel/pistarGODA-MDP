# Extension of Goal-Oriented Dependability Analysis (GODA) framework
This project extends [GODA framework](https://github.com/lesunb/CRGMToPRISM/) into supporting the goal modeling of SAS under multiple sources of uncertainty, and the generation of symbolic parametric formulae with parameterized uncertainties. The following code extends the [piStar tool](http://www.cin.ufpe.br/%7Ejhcp/pistar/#) and provides a modeling and analysing environment in the web for GODA. The pistarGODA modeling and analyzing environment is available online at [Heroku](https://seams2019.herokuapp.com/).

## Modeling Menu

### New model 
Clear the default model > Insert an actor > Insert the model's goals, tasks and their refinements (AND/OR).

### Load model
Load existent txt file.

### Save file
Save the model in a txt file for further use.

### Parametric formula generation
Once the goal model is finished, generate its reliability and cost parametric formulas (files result.out and cost.out, respectively).

### Generate PRISM MDP code
Once the goal model is finished, generate its correspondent MDP model in PRISM language.

## Adding properties

Your root goal should **always** have the "selected true" property:
* Click on the root goal > Add property > Name: *selected* > Value: *true*

To add a **context condition** in a **goal**:
* Click on the specific goal > Add property > Name: *creationProperty* > Value: *assertion condition **context***

To add a **context condition** in a **task**:
* Click on the specific task > Add property > Name: *creationProperty* > Value: *assertion trigger **context***

The ***context*** has a specific regex to be followed.

## Context condition regex
Context conditions are formed by a proposition of context facts. You must use valid PRISM operators to define your context facts and to compose a valid context annotation. Valid operators are: 

<, <=, >, >=, =, !=, &, |

Operands are the context facts and the values they can be compared with. Context facts are represented as meta-variables. They must start with normal, non-digit characters and may end with digits. Values can be either boolean (true or false), integer or double. 

Valid context annotation examples are:

* BATTERY >= 50
* CONNECTION = true
* BATTERY >= 50 & CONNECTION = true

## Syntax used to label Goals and Tasks 

Goals and tasks must have a lable according to the rules bellow. The general form of a lable is:

* *IDENTIFIER*: *DESCRIPTION* *[decision-making annotation]*

### *IDENTIFIER*: *DESCRIPTION*

* Goals must use a **prefix G** followed by an **unique numeric ID** within the CRGM Goals set followed by a **colon** followed by a **textual description** . Ex: G1: Goal description, G2: Goal description, G3: Goal description

* Same rule used for Goals, except that:
	* Tasks must use a **prefix T** 
	* Task level refers to the depth level after the goal they succeed
	* Level 1 tasks' prefix should be followed by an **unique numeric ID** within their context. Ex:
		* T1: Task description (for goal **G2**)
		* T1: Task description (for goal **G3**)
	* Second and subsequent level tasks prefix must be followed by the same **unique numeric ID** of its first level task, a **dot** and an **unique numeric ID**. Ex:
		* T1.1: Tsk Dsc, T1.2: Tsk dsc (descendants of T1)
		* T1.11: Tsk dsc, T1.12: Tsk dsc (descendantas of T1 -> T1.1) 
		* T1.111: Tsk dsc, T1.112: Tsk dsc, T1.113: Tsk dsc... (descendants of T1 -> T1.11) and so forward.

### *[Decision-making annotation]*

* Any non-leaf node (goal or task) that is refined/decomposed into two or more sub-nodes can have a *decision-making annotation* as part of its label. A node with this annotation can be satisfied by any combination of its specified children.
* *decision-making annotations* must be inside brakets and be placed after *DESCRIPTION*. Ex:
* G1:Goal description **[DM(G2,G3,G4)]**, meaning that the fulfillment of any combination of G2, G3, G4 will result in the fulfillment of G1.
* *decision-making annotations* must be separated from *DESCRIPTION* with space(s). As long as it is inside the brakets and after *DESCRIPTION*, it will be parsed.

### *Weigth function*

* Any leaf-task can be annotated with a weigth function that determines the cost/reward of executing such node.
* The weight function regex is: *W = value*, in which *value* can be either a double, a string variable, or both.
* The weight function must be inside brackets and be placed after *DESCRIPTION*. Ex: T1.1: Leaf-task description **[W = 0.1x]**
* The weight function must be separated from *DESCRIPTION* with space(s). As long as it is inside the brakets and after *DESCRIPTION*, it will be parsed.

## Development

### Updating an ANTLR grammar

* After updating any .g4 grammar file (for context or runtime annotations), use maven to recompile the java classes for your language (regex, parser, etc):
	* At the project root folder, use (linux): mvn install -Dmaven.test.skip=true
	* At the project root folder, use (linux): mvn package -e -Dmaven.test.skip=true
	* Refresh the project folder in your IDE (Eclipse, etc.)

## Bugs? Doubts?

* Look for existing issues or create a new one describing your problem or doubt
* Contact the author by email
	* felix.solano[at]gmail[dot]com
	* TODO: add other team members contacts here
