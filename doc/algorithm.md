# Transfert Reliability Margin (TRM) algorithm

This document describes the current state of the algorithm.

## Algorithm steps

### Critical Network Element (CNE) selection

#### CNE selection

The CNE are selected using the `XnecProvider` interface.  
This means that you can provide your own list of branches or automatically select interconnections or branches with more than 5% zonal PTDF.  
You can also do the union or the intersection of such providers.  
You can also implement your own provider.  
Note that in the TRM algorithm, the contingencies are not used.  
> We do not use the CNE available in the CRAC

Hvdc lines cannot not be selected because they are not branches, and we cannot compute their sensitivity.  

The CNE are selected by running the provider on the reference network.  
The selected CNE have to be available in the market-based network.  
This means that the reference network can be a geographical subnetwork (Spain and France for example) of the market-based network (SWE).

### Operational condition alignment

The operational conditions of the market-based network are aligned with those of the real time snapshot.  
The preventive range actions of these operations are not respected.
> This simple behavior might change later.  

We assume that the IDs of each network element remains the same between the market-based network and the real time snapshot.  

We create a pipeline with desired operational condition aligner.  
Here are the available operational condition aligners:

#### Crac alignment

We apply each network action that has been applied to the real time snapshot to the market-based network.
> We ignore the usage rules of the network actions. This means that not only preventive network actions will be used, but also curative ones

#### Matching HVDC mode and power

For each HVDC line on the market-based network, their mode and power are aligned with their corresponding HVDC line on 
the real time snapshot.  
A warning is issued when some HVDC lines do not have any corresponding HVDC line on the other line.  
> An HVDC modeled with its AC representation (two generators and loads) will not be aligned.  

#### PST alignment

For each PST on the market-based network, their tap is aligned with their corresponding PST on the real time snapshot.  
A warning is issued when some PSTs do not have any corresponding PST on the other network.  
Ratio tap PST and phase tap PST are aligned.

#### Dangling line aligner

We want to align exchanges with countries that are not included in the reference network.  
- Given an unpaired reference dangling line,
  - If the market-based dangling line is also not paired, we align the market-based dangling line target P.
  - If the market-based dangling line has been paired, the market-based tie line will be unpaired, and we align the 
  corresponding unpaired market-based dangling line target P.
- Given a paired reference dangling line,
  - If the market-based dangling line is also paired, we ignore this dangling line.
  - If the market-based dangling line is not paired, we throw an issue.

#### Align inner exchanges

The goal is to align the exchanges of market-based network on the exchanges of the real time snapshot for each boundary 
of the real time snapshot.  
The alignement is done with balances adjustment.  
The alignment is ignored if the exchanges are already the same.  

$`c`$ is a country in the market-based network.  
$`NP_{target}(c)`$ is the target net position of a country $`c`$.  
This net position will be the goal of the balance adjustment.  
$`exchange_{networkN}(countryA, countryB)`$ defines the leaving flow from country $`countryA`$ to country $`countryB`$ 
in the network $`networkN`$.  
If the country is not present in the real-time snapshot, the net position remains the same.  
Otherwise, $`NP_{target}(c) = NP_{market} + \sum_{otherCountry!=c}exchange_{real-time}(c, otherCountry) - \sum_{otherCountry!=c}exchange_{market-based}(c, otherCountry)`$

This formula allows the alignement to be valid even with a real time network that might be a subpart of the market-based 
network.  
If the networks have the same countries, this is equivalent to setting the target net positions to the real-time net 
positions.  
If the network is not for a NTC process and if a smaller network is used, the alignement might fail with a status `TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED`.  
> This simple bahavior might be changed in the futur is required. But it does not make sens from a métier point of view as the afrr concept exists. 

#### HVDC AC modeling environment

If you need to transform your HVDC equivalent model to HVDC, we provide an environment.  
When we enter the environment, the HVDC equivalent models are turned into HVDC.  
Then a specified aligner is called (for example aligning exchanges).  
Then the HVDC are turned into their equivalent models.  
We do not keep HVDC in the network to be able to compute their PTDF.  
> Warning, if you have HVDC in DC mode, this environment might reset their set point !

#### Pipeline example

Here is an example of a pipeline 

```java
CracAligner cracAligner = new CracAligner(crac);
HvdcAligner hvdcAligner = new HvdcAligner();
PstAligner pstAligner = new PstAligner();
DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
ExchangeAligner exchangeAligner = new ExchangeAligner(balanceComputationParameters, loadFlowRunner, computationManager, marketBasedScalable);
HvdcAcModelingEnvironment hvdcAcModelingEnvironment = new HvdcAcModelingEnvironment(creationParametersSet, exchangeAligner);
OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(cracAligner,
        hvdcAligner,
        pstAligner,
        danglingLineAligner,
        hvdcAcModelingEnvironment);
```

### Flow extraction

For all selected lines, we can extract:
- $`P_{A,CC}(i)`$ the AC power flow of line $`i`$ in the aligned market-based network
- $`P_{RT}(i)`$ the AC power flow of line $`i`$ in the real time snapshot network
> By default, we use the terminal 1 flow as power flow. 

### Sensitivity computation

For all selected lines, we can compute $`PTDF(i)`$ the zonal PTDF in the real time snapshot network with an AC 
sensitivity computation.  
> By default, we use the terminal 1 during the sensitivity analysis.

The HVDC are modeled with their equivalent model to be able to compute their zonal PTDF.  
> Therefore, HVDC saturations are ignored. 

### Uncertainties computation

The uncertainty of a given line $`i`$ is 

$`UN(i)=\frac{P_{A,CC}(i)-P_{RT}(i)}{PTDF(i)}`$

## Inputs

### Market-based network

The market-based network is either a D2CF network or a DACF network.  
This network models what the network is expected to be at a future timestamp from a market point of view.  
It does not take into account the last minute operations on the network.
> TODO: validate this description

### Real time snapshot network

The real time snapshot represents what has really happened on the network.
> TODO: validate this description

### Real time GLSK

The real time GLSK are required to compute the zonal PTDF.

### Real time CRAC

The CRAC contains the network actions. 

## Outputs

### Export results to CSV

The results can be exported to a CSV.

Here is the format :

    Case date;Branch ID;Branch name;Country Side 1;Country Side 2;Uncertainty;Market-based flow;Reference flow;Zonal PTDF
    2024-07-15T13:14:12Z[UTC];toto;FGEN1 11 BLOAD 11 1;FR;BE;12.0;100.0;112.0;-1.0

