# sim-aleh-explainer-muict-internship-2026

---

## Project Overview
SimExplainer is a Java library for computing concept similarity in Description Logic (DL) ontologies. It supports four DLs — **ELH**, **ALEH**, **FL0**, and **ALC** — each with its own similarity measure implemented according to its respective paper, all callable through a single, uniform API. The library supports both OWL and KRSS file formats and offers a rich set of features for loading, processing, and explaining concept similarities based on user-defined preference profiles. The project aims to provide researchers and developers with a powerful tool for semantic similarity analysis in various domains.

**Which measure to use:**
- **ELH / ALEH** — structural matching under an agent's preference profile (importance, similarity, and role-discount functions). ALEH additionally supports value restrictions (`∀r.C`) and atomic negation (`¬A`), which ELH does not.
- **FL0** — a role-set-based similarity measure for the DL FL0 (concepts built only from `∀r.C` restrictions), following its own paper's formula.
- **ALC** — a dissimilarity-based measure (Def. 4.1/4.2, d'Amato/Fanizzi/Esposito) supporting full boolean concept construction (`⊓`, `⊔`, `¬`, `∃`, `∀`), converted to a similarity score internally.

---

## Usage

To use the `SimExplainer` library, follow these steps:

### 1. Instantiate the SimExplainer

You can instantiate the `SimExplainer` using different sets of input files.

**Using a Directory Containing Both Ontology and Preference Profile Files**
```java
SimExplainer explainer = new SimExplainer("path/to/ontologyAndProfileDirectory");
```

**Using Separate Directories for Ontology and Preference Profile Files**
```java
SimExplainer explainer = new SimExplainer("path/to/ontologyDirectory", "path/to/preferenceProfileDirectory");
```

**Using Individual File Paths**
```java
SimExplainer explainer = new SimExplainer(
    "path/to/ontologyFile",
    "path/to/primitiveConceptImportanceFile",
    "path/to/roleImportanceFile",
    "path/to/primitiveConceptsSimilarityFile",
    "path/to/primitiveRolesSimilarityFile",
    "path/to/roleDiscountFactorFile"
);
```
**Every path, except the Ontology path, can be left as null.*

**Input Files**

When initializing the `SimExplainer` with a directory, the following files will be automatically read if present:

- **Ontology file**: The last file found with the extension `.owl` or `.krss` will be used.
- **Primitive Concept Importance file**: The last file that starts with "primitive-concept-importance" will be used.
- **Role Importance file**: The last file that starts with "role-importance" will be used.
- **Primitive Concept Similarity file**: The last file that starts with "primitive-concepts-similarity" will be used.
- **Primitive Role Similarity file**: The last file that starts with "primitive-roles-similarity" will be used.
- **Role Discount Factor file**: The last file that starts with "role-discount-factor" will be used.

**Preference Profile Files**

You can load these files manually using the following methods:
```java
void ReadInputPrimitiveConceptImportances(String pathToFile) throws IOException
void ReadInputRoleImportances(String pathToFile) throws IOException
void ReadInputPrimitiveConceptsSimilarities(String pathToFile) throws IOException 
void ReadInputPrimitiveRolesSimilarities(String pathToFile) throws IOException
void ReadInputRoleDiscountFactors(String pathToFile) throws IOException
```

If you didn't set a Role Discount Factor, there is a default value provided (default = 0.4). But you can still set the default value with this method:
```java
void setDefaultRoleDiscountFactor(BigDecimal value)
```

You can reset the preference profile with this method:
```java
void resetPreferenceProfile()
```

> **Note:** the preference profile above applies to ELH/ALEH/FL0. ALC's similarity measure (Def. 4.1/4.2) does not use a preference profile at all — the source paper does not define one for ALC's structural comparison, so ALC's discount factor (analogous to the ELH-family's role-discount factor) is a fixed level-based value internal to `ALCPreferenceProfile`, not something read from the preference profile files above.

### 2. Retrieve Concept Names from the Loaded Ontology

This retrieves all concept names from the loaded ontology.
```java
List<String> conceptNames = explainer.retrieveConceptName();
conceptNames.forEach(System.out::println);
```

### 3. Measure Similarity Between Concept Names in the Loaded Ontology

This measures the similarity between two concepts.
```java
BigDecimal similarity = explainer.similarity(ImplementationMethod.TOPDOWN_ALEH_SIM, "Concept1", "Concept2");
System.out.println("Similarity: " + similarity);
```

By default, the forward and backward directional scores are combined by averaging. You can choose a different combination strategy with the overload:
```java
BigDecimal similarity = explainer.similarity(ImplementationMethod.TOPDOWN_ALEH_SIM, "Concept1", "Concept2", CombinationStrategy.AVERAGE);
```

**`ImplementationMethod`** — every measure supported by the library:

| DL | Constant | Description |
|---|---|---|
| ELH | `DYNAMIC_SIM` | dynamic programming, no preference profile |
| ELH | `DYNAMIC_SIMPI` | dynamic programming, with preference profile |
| ELH | `TOPDOWN_SIM` | top-down, no preference profile |
| ELH | `TOPDOWN_SIMPI` | top-down, with preference profile |
| ALEH | `DYNAMIC_ALEH_SIM` | dynamic programming, no preference profile |
| ALEH | `DYNAMIC_ALEH_SIMPI` | dynamic programming, with preference profile |
| ALEH | `TOPDOWN_ALEH_SIM` | top-down, no preference profile |
| ALEH | `TOPDOWN_ALEH_SIMPI` | top-down, with preference profile |
| FL0 | `DYNAMIC_FL0_SIM` | dynamic programming, no preference profile |
| FL0 | `DYNAMIC_FL0_SIMPI` | dynamic programming, with preference profile |
| FL0 | `TOPDOWN_FL0_SIM` | top-down, no preference profile |
| FL0 | `TOPDOWN_FL0_SIMPI` | top-down, with preference profile |
| ALC | `TOPDOWN_ALC_SIM` | top-down, per Def. 4.1/4.2 (no preference-profile variant) |

**`CombinationStrategy`** — how the forward and backward directional scores are combined into one symmetric score:

- `AVERAGE` — `(forward + backward) / 2` (default)
- `MULTIPLICATION` — `forward * backward`
- `RMS` — root mean square of forward and backward

### 4. Retrieve Tree Hierarchy

This retrieves the tree hierarchy for a given concept.
```java
String hierarchy = explainer.treeHierarchy("Concept1");
System.out.println("Tree Hierarchy: " + hierarchy);
```

> **Note:** this only works for concepts that have already been compared via an **ELH or ALEH** `similarity()` call — it does not work after a FL0-only or ALC-only session, since those measures don't populate the shared tree-based explanation map. If you need the description tree for a concept compared only under FL0 or ALC, run at least one ELH/ALEH `similarity()` call for that concept first.

### 5. Retrieve Explanation

How you retrieve an explanation depends on which DL you used, since ELH/ALEH, FL0, and ALC each produce a differently-shaped explanation (see [Reading Explanation](#reading-explanation) below for the full format of each).

**ELH / ALEH**
```java
SimExplainer.Explanation explanation = explainer.getExplanation(
        "Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALEH_SIM, CombinationStrategy.AVERAGE, true);
System.out.println("Similarity Degree: " + explanation.similarity);
System.out.println("Forward Explanation: " + explanation.forward);
System.out.println("Backward Explanation: " + explanation.backward);
```
The last boolean argument controls whether fresh concept names (see [Fresh Concept Names](#fresh-concept-names) below) are shown with their trailing `'`.

**FL0**
```java
FL0BacktraceTable fl0Table = explainer.getFL0Explanation(
        "Concept1", "Concept2", ImplementationMethod.TOPDOWN_FL0_SIM, CombinationStrategy.AVERAGE);
System.out.println("Forward: " + fl0Table.printForward());
System.out.println("Backward: " + fl0Table.printBackward());
```

**ALC**
```java
ALCExplanationTable alcTable = explainer.getALCExplanation(
        "Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALC_SIM, CombinationStrategy.AVERAGE);
System.out.println("Forward: " + alcTable.printForward());
System.out.println("Backward: " + alcTable.printBackward());
```

In all three cases, you must call `similarity(...)` with the matching concept pair, method, and strategy **first** — the explanation is cached from that call, not recomputed.

### 6. Retrieve Explanation as JSON

*(ELH/ALEH only — see note below.)*

This retrieves the explanation as a JSON object.
```java
JSONObject explanationJson = explainer.getExplanationAsJson("Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALEH_SIM, CombinationStrategy.AVERAGE);
System.out.println(explanationJson.toString(4));
```

**Save Explanation as JSON**

This saves the explanation as a JSON file.
```java
JSONObject explanationJson = explainer.getExplanationAsJson("Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALEH_SIM, CombinationStrategy.AVERAGE, "path/to/outputFile.json");
```

> **Note:** JSON export and natural-language conversion (below) are currently only wired up for the ELH/ALEH explanation path. FL0's `FL0BacktraceTable` and ALC's `ALCExplanationTable` do not have JSON export methods yet — use `printForward()`/`printBackward()` for those (see above).

### 7. Convert Explanation to Natural Language

*(ELH/ALEH only, same caveat as above.)*

To use the following function, you have to set up the OpenAI API Key and API Response Timeout.
```java
void setApiTimeout(int apiTimeout); // in seconds, default is 45
void setApiKey(String apiKey); // must set this
```

This converts the explanation for the similarity between two concepts into natural language.
```java
JSONObject naturalLanguageExplanation = explainer.getExplanationAsNaturalLanguage("Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALEH_SIM, CombinationStrategy.AVERAGE);
System.out.println(naturalLanguageExplanation.toString(4));
```

**Save Explanation as Natural Language**

This saves the natural language explanation as a JSON file.
```java
JSONObject naturalLanguageExplanation = explainer.getExplanationAsNaturalLanguage("Concept1", "Concept2", ImplementationMethod.TOPDOWN_ALEH_SIM, CombinationStrategy.AVERAGE, "path/to/outputFile.json");
```

---

## Fresh Concept Names

Concepts with no explicit definition in the ontology (or that are primitive) are given a "fresh" internal name suffixed with `'` (e.g. `MargheritaPizza'`) so that unfolding is guaranteed to terminate. Several methods (`getExplanation`, `treeHierarchy`, `treeHierarchyAsJson`, `getExplanationAsJson`) accept an `includeFreshConceptName` boolean to control whether this `'` suffix is shown in the output — pass `true` to see it, `false` to hide it. The underlying similarity score is identical either way; the flag only affects display.

---

## Reading Explanation

### Description Tree

A Description Tree provides a structured way to represent concepts and their relationships in a hierarchical format. This can be represented in JSON for machine readability and in ASCII for human readability.

#### Understanding the Description Tree

**JSON Representation**

```json
{
   "conceptName": "RootConcept",
   "primitiveConcepts": ["PrimitiveConcept"],
   "existentials": [
      {
         "conceptName": "ChildConcept1",
         "primitiveConcepts": ["PrimitiveConcept1"],
         "roleName": "roleName1",
         "existentials": [
            // Further child concepts
         ]
      },
      {
         "conceptName": "ChildConcept2",
         "primitiveConcepts": ["PrimitiveConcept2"],
         "roleName": "roleName2",
         "existentials": []
      }
   ]
}
```

**Components:**

- **conceptName**: The name of the concept.
- **primitiveConcepts**: A list of primitive concepts that define the concept in the current subtree.
- **existentials**: A list of child concepts, each with its own structure including conceptName, primitiveConcepts, roleName, and further existentials.

**ASCII Representation**

```
└── RootConcept : [PrimitiveConcept]
    ├── roleName1 : [PrimitiveConcept1]
    │   └── ChildConcept1
    └── roleName2 : [PrimitiveConcept2]
        └── ChildConcept2
```

- **RootConcept**: The main concept at the root of the tree.
- **[PrimitiveConcept]**: The primitive concepts that define the root concept.
- **roleName1, roleName2**: The roles that connect the root concept to its child concepts.
- **ChildConcept1, ChildConcept2**: The child concepts connected to the root concept via specific roles.
- **Indentation and Lines**: used to show the hierarchical structure and relationships.

---

### Explanation Tree — ELH / ALEH

An Explanation Tree provides a structured way to represent the comparison between two concepts and their relationships in a hierarchical format. This can be represented in JSON for machine readability and in ASCII for human readability.

#### Understanding the SimRecord

A SimRecord is the core structure used to describe the similarity between two concepts in the Explanation Tree. It includes:

- **deg**: The homomorphism degree between the concepts.
- **pri**: A list of pairs of primitive concepts from each concept that contribute to the similarity degree. The first component in each pair is from the first concept, and the second component is from the second concept.
- **exi**: A list of pairs of existential (`∃r.C`) concepts from each concept that contribute to the similarity degree.
- **uni**: A list of pairs of universal (`∀r.C`) concepts from each concept that contribute to the similarity degree. *(ALEH only — ELH has no value restrictions, so this is always empty under an ELH measure.)*
- **emb**: A map where the key is a pair of existential/primitive concept pairs, and the value is a set of pairs of roles or primitive concepts that have found similarity within the embedding space. This key-value structure enhances the similarity degree by showing detailed semantic relationships.

> `dis` also appears in the ASCII/`toString()` output of a SimRecord (reserved for disjunction-pair matches), but is not currently populated by the ELH/ALEH measures and is always empty — it exists for structural symmetry with the shared `Tree`/`TreeNode` framework and is not part of either the ELH or ALEH paper's formula.

**Example of SimRecord**

Here is an example SimRecord and how to read it:

**Explanation in one direction of 'ActivePlace' and 'Mangrove'**

```json
{
    "pri": ["(Place, Place)"],
    "deg": 0.8259457964,
    "exi": [
        "(some canSail Kayaking, some canWalk Trekking)",
        "(some canWalk Trekking, some canWalk Trekking)"
    ],
    "emb": {
        "(some canSail Kayaking, some canWalk Trekking)": [{
            "first": "canTravelWithSail",
            "second": "canMoveWithLegs"
        }]
    }
}
```

**Explanation:**

- **deg**: The homomorphism degree is 0.8259457964. This value indicates how similar the two concepts are.
- **pri**: The primitive concept "Place" in "ActivePlace" is compared with "Place" in "Mangrove". Since they are the same, it increases the similarity degree.
- **exi**:
   - The existential concept "(some canSail Kayaking, some canWalk Trekking)" is compared. Since "canSail Kayaking" and "canWalk Trekking" are different, embeddings are examined.
   - The existential concept "(some canWalk Trekking, some canWalk Trekking)" is the same, which further increases the similarity degree.
- **emb**:
   - For the pair "(some canSail Kayaking, some canWalk Trekking)", the roles "canTravelWithSail" for "Kayaking" and "canMoveWithLegs" for "Trekking" are found to be similar in the embedding space. This contributes to the similarity degree.

#### Explanation Tree Formats

**JSON Representation**

```json
{
    "similarity": similarity_score,
    "forward": { /* forward explanation object */ },
    "backward": { /* backward explanation object */ }
}
```

The main similarity score is derived by combining the similarity degrees from both the forward and backward explanations, using the chosen `CombinationStrategy` (default is average):

\[ \text{similarity\_score} = \frac{\text{forward\_deg} + \text{backward\_deg}}{2} \]

A forward/backward explanation object has the following structure:

```json
{
    "children": [ /* explanation object (recursive structure) */ ],
    "pri": [ /* set of primitive concept pairs from SimRecord */ ],
    "deg": similarity_degree,
    "exi": [ /* set of existential pairs from SimRecord */ ],
    "comparingConcept2": "Concept2",
    "emb": { /* map of embeddings from SimRecord */ },
    "comparingConcept1": "Concept1"
}
```
(Note: as mentioned above, `uni` is populated by the ALEH measure but is not currently included in this JSON representation — it is visible in the ASCII output only.)

**ASCII Representation**

**Explanation in one direction (A -> B):**

```
└── [ConceptA] : [ConceptB] - SimRecord{deg=similarity_degree, pri=[(PrimitiveConceptX, PrimitiveConceptY)], exi=[(ExistentialX, ExistentialY)], uni=[], dis=[], emb={}}
    ├── [ConceptC] : [ConceptD] - SimRecord{deg=similarity_degree, pri=[(PrimitiveConceptC1, PrimitiveConceptD1)], exi=[(ExistentialC1, ExistentialD1)], uni=[], dis=[], emb={(ExistentialC1, ExistentialD1): [(RoleC1, RoleD1)]}}
```

- **[ConceptA] : [ConceptB]**: indicates the similarity between `ConceptA` and `ConceptB` in one direction (A -> B). The SimRecord describes the details of this similarity.

---

### Explanation Tree — FL0

FL0's explanation is a flat, single-level structure — it reports the full sets of matched and missed primitive concepts across the entire concept comparison, rather than a recursive tree of sub-comparisons (FL0 concepts have no existential restrictions, only nested `∀r.C`, so the recursion collapses into one flattened comparison of the concepts' role-value sets).

#### Understanding the FL0Record

Retrieved via `FL0BacktraceTable.printForward()` / `printBackward()`, an `FL0Record` reports:

- **deg**: the FL0 similarity degree for that direction.
- **matched**: primitive concepts present in both sides' fully-expanded definitions.
- **missed**: primitive concepts present on one side but not matched on the other — the concepts responsible for `deg` being less than 1.

**Example**
```
FL0Record{deg=0.95238, matched=[Pizza, PizzaTopping, ... ], missed=[OliveTopping', ParmesanTopping', SohoPizza']}
```
Here, forward similarity is 0.95238 because `OliveTopping'`, `ParmesanTopping'`, and `SohoPizza'` appear on one side without a corresponding match on the other — every other primitive matched.

---

### Explanation Tree — ALC

ALC's explanation follows the source paper's own overlap/dissimilarity structure (Def. 4.1/4.2) rather than the ELH-family's homomorphism-degree structure, since ALC's measure is fundamentally different (dissimilarity-based, with disjunction support). Retrieved via `ALCExplanationTable.printForward()` / `printBackward()`.

#### Understanding the SimRecord / DisSimRecord pairing

Each line pairs a **similarity** view (`deg = 1 − dissimilarity`) with the underlying **DisSimRecord**, which shows the raw dissimilarity components:

- **deg** (outer `SimRecord`): the similarity for this node pair, `1 − dissimilarity`.
- **deg** (inner `DisSimRecord`): the raw dissimilarity for this node pair.
- **case**: how this node pair was resolved — `EQUIVALENT` (structurally identical, dissimilarity 0), `DISJOINT` (one side unsatisfiable, dissimilarity 1), or `RECURSIVE` (computed via the overlap function).
- **branch**: which disjunct pairing produced the reported result — `single_disjunct` if neither side is a union, or `(C<i>,D<j>)` naming the winning disjunct indices when either side is a disjunction (`⊔`).
- **pairs**: the number of disjunct pairs compared to find the best match — `|disjuncts(C)| × |disjuncts(D)|`. A value greater than 1 confirms disjunction was actually compared (this is what proves the union-filler bug described below was fixed — e.g. `pairs=8` for a 2-disjunct vs. 4-disjunct union comparison).
- **pri**: primitive concepts contributing to the node-level overlap.
- **exi** / **uni**: roles (not concept pairs, unlike ELH/ALEH) with existential/universal restrictions contributing to the overlap at this node.
- **missed**: existential fillers on the larger side that had no counterpart on the smaller side (see `f_exists`'s asymmetric matching rule below).

Nested lines (`ALL <role>: ...`, `EXISTS <role> [i]: ...`) are recursive calls into the sub-structures reached via that role — every line is an independently-computed node comparison, not a summary.

**Example** (`MargheritaPizza` vs `SohoPizza`, where `hasTopping only (Mozzarella or Tomato)` is compared against `hasTopping only (Mozzarella or Olive or Parmesan or Tomato)`):
```
[MargheritaPizza] : [SohoPizza]
SimRecord{deg=0.9091, DisSimRecord{deg=0.0909, case=RECURSIVE, branch=single_disjunct, pairs=1, pri=[...], exi=[hasTopping,hasCaloricContent,hasBase], uni=[hasTopping], missed=[]}}
  ALL hasTopping: SimRecord{deg=0.5000, DisSimRecord{deg=0.5000, case=RECURSIVE, branch=(C1,D2), pairs=8, pri=[PizzaTopping], exi=[hasSpiciness], uni=[], missed=[]}}
  EXISTS hasTopping [0]: SimRecord{deg=0.5000, ...}
  EXISTS hasTopping [1]: SimRecord{deg=0.5000, ...}
  ...
```
The `ALL hasTopping` line's `pairs=8` shows all 2×4=8 disjunct pairings were compared, and `branch=(C1,D2)` names which pairing won. Four `EXISTS hasTopping [i]` lines appear (not two) because ALC's existential matching rule (`f_exists`, Def. 4.1) always produces `max(|C's existential list|, |D's existential list|)` terms — SohoPizza's 4 `hasTopping some X` restrictions outnumber MargheritaPizza's 2, so 4 best-match comparisons are made regardless of comparison direction; this can mean the same smaller-side filler is reused as the "best match" for more than one larger-side filler.

**Important scope note:** ALC's similarity is derived (`1 − dissimilarity`) but its underlying measure is *asymmetric by design* (`f(C,D) ≠ f(D,C)` in general — Def. 4.1's existential-matching rule depends on which side has more restrictions), which is why the forward and backward `deg` in the example above are equal by coincidence at the root but the *recorded winning disjunct pair* (`(C1,D2)` vs `(C3,D0)`) differs between directions even at equal scores — this reflects genuine ties among equally-good disjunct pairings, not an error.

---

## Known Issues / Limitations

- **ALEH, no-preference-profile variants (`_SIM`, not `_SIMPI`)**: the ALEH paper itself only defines the homomorphism degree in terms of a preference profile π (Definition 5) — it does not give a separate "no preference" formula. The `_SIM` methods in this library are the degenerate case of that same formula with concept/role importance fixed at 1 and concept/role similarity fixed to exact-match-only; the role discount factor still defaults to 0.4 per the paper's own Eq. 3, but is drawn from whatever preference profile is loaded in the current session (so calling `ReadInputRoleDiscountFactors(...)` before an `_SIM` call will still affect it — see the code for `dHat()` in `TopDownALEHSimReasonerImpl`).
- **FL0**: implemented per the FL0 paper's own formula (`f_P`, `f_∀`, `f_∃`), but the underlying measure can produce results that are sensitive to how deeply a concept's `∀`-restrictions are nested, independent of ontology-author intent — this is a property of the FL0 measure itself (see the paper) rather than an implementation defect.
- **ALC**: implemented per Def. 4.1/4.2 (d'Amato/Fanizzi/Esposito); the source paper does not define a preference-profile variant for ALC, so `TOPDOWN_ALC_SIM` is the only ALC method offered.

---

## Principal Investigator
- Teeradaj Racharak (Tohoku University, Japan)
   - Email: racharak@tohoku.ac.jp