# Transfert Reliability Margin (TRM) algorithm

This document describes the current state of the algorithm.

## Algorithm steps

### Critical Network Element (CNE) selection and filtering

#### CNE selection

All the interconnections are considered to be CNEs.
> This simple behavior might change later.
> Hvdc lines are not selected because we cannot compute their sensitivity

### Operational condition alignment

The operational conditions of the market-based network are aligned with those of the real time snapshot.  
The preventive range actions of these operations are not respected.
> This simple behavior might change later.  

We assume that the IDs of each network element remains the same between the market-based network and the real time snapshot.
> This simple behavior might change later.

#### Crac alignment

We apply each network action that has been applied to the real time snapshot to the market-based network.
> We ignore the usage rules of the network actions. 

#### PST alignment

For each PST on the market-based network, their tap is aligned with their corresponding PST on the real time snapshot.  
A warning is issued when some PSTs do not have any corresponding PST on the other network.

#### Adjusting topology

> We do not change the topology yet.  
> This simple behavior might change later.

#### Matching HVDC mode and power

For each HVDC line on the market-based network, their mode and power are aligned with their corresponding HVDC line on 
the real time snapshot.  
A warning is issued when some HVDC lines do not have any corresponding HVDC line on the other line.

#### Shift exchanges

The goal will be to shift exchanges on a given boundary and reset other boundary exchanges
> We do not have implemented this yet.  
> This simple behavior might change later.

### Flow extraction

For all selected lines, we can extract:
- $`P_{A,CC}(i)`$ the AC power flow of line $`i`$ in the aligned market-based network
- $`P_{RT}(i)`$ the AC power flow of line $`i`$ in the real time snapshot network
> By default, we use the terminal 1 flow as power flow. 

### Sensitivity computation

For all selected lines, we can compute $`PTDF(i)`$ the zonal PTDF in the real time snapshot network with an AC 
sensitivity computation.  
> By default, we use the terminal 1 during the sensitivity analysis.

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


