AggFlow : SDN network hypervisor with enhanced address virtualziation
============
- We improved address virtualization and flow rule management based on the representative SDN network hypervisor, OpenVirteX.
- The main mechanisms of implementation can be found on our paper. (AggFlow in CAN’16).
- Contacts
  * Libera team (Korea University) : sdn_os@korea.ac.kr
  * Main developer (Bong-yeol Yu) : koreagood13@gmail.com

Functions
-------------
-	Mapping-less address virtualization: In network virtualization, a virtual address space must be separated between tenants and the overlapping addresses should be able to distinguished. Our approach distinguishes the virtual networks by just setting the TID at the source MAC address, not mapping the virtual addresses to physical addresses. The information containing at the source MAC address field can be identified with the in_port or source IP address field.

-	Hop-by-hop forwarding: Furthermore, we implement hop-by-hop-based forwarding, which aggregates flow rules to be installed in physical switches. To reduce rules installed in physical switches, we introduce the hop-by-hop-based forwarding. In core switches, the rules that have same ingress and same egress port are aggregated. Particularly, when 3 switches are linear, just one rule is installed in the center switch. In the result of that, we reduce the number of rule installed physical switches.

Code modifications
-------------
- More specifically, we changed the actions of flowmod message sent to physical switch to rewrite the TID at the source MAC address (Mapping-less address virtualization), and the ID of the next switch to the destination MAC address (hop-by-hop based forwarding). Then the core switches only check the in-port and the destination MAC address. This approach reduces the number of rules installed in the physical switches. In addition, in order to avoid the installation of duplicated rules, we implemented PhysicalFlowTable and PhysicalFlowEntry object, that maintains the flow table of physical switches. With the objects, we checks the duplicated rule exists or not. If the flow rule with the same matching fields and actions exist, our implementation ignores the installation of it. If the matching fields are same, but the actions are different, we extend the matching fields up to IP address fields to distinguish the new rule. The details of the overall process can be found in our paper (AggFlow in CAN’16).

- We modified or newly created the following implementations for adapting the techniques.

1. Newly created class files.
 * PhysicalFlowEntry.java : Objects that represent the flow table entry.
 * PhysicalFlowTable.java : Objects that manage and store the rules, which are installed in the physical switches, so that the rules are not redundantly installed.
 
2. Modified class files.
  * PhysicalSwitch.java : Added a PhysicalFlowTable instance and methods for it.
  * OVXLink.java : Modified the routine as to modify matching fields and actions of the flow rule, which is to be installed at the physical switch contained in the OVXLink.
  * OVXLinkUtils.java : Modified the constructor to have the MAC addresses following the technique, and removed the part of adding the source MAC addresses action in the setLinkfiled method.
  * OVXNetwork.java : Added the methods that fetch host instance using the IP addresses and MAC addresses.
  * OVXActionOutput.java
    * Modified the routine to find the physical switch and directly assign its id when the switch is a big-switch. (without using lUtils)
    * Added the OVXSwitch instance as a parameter of the lUtils instance.
  * OVXFlowMod.java
    * Added the routine - when the match has not the IP address information, finds the host which owned the IP addresses and puts the IP addresses information to the match field.
    * Added the routine - fetch the PhysicalFlowTable instance tied in the physical switch.
    * Added the routine - when the in-port and out-port are the edge, extends the matching field up to IP addresses.
    * Removed  the part of IP rewrite match and IP action.
    * Added the routine - in the core switches, checks the rules are duplicated and if it is not duplicated, then the flowmod is sent to the physical switch.
    * Added the method that checks the out-port is an edge.
    * Added the method that finds IP addresses of the hosts using the MAC addresses
  * OVXFlowRemoved.java
    * Added the routine that fetches the PhysicalFlowTable tied in the physical switch.
    * Added the routine that fetches the cookies that aggregated to the cookie of the rule generating the FlowRemoved message and then each FlowRemoved message is sent to the controller.
  * OVXPacketIn.java
    * Modified the routine – when the PacketIn message of the core switch arrives, the routine recovers the original address.
  * SwitchRoute.java
    * Replaced the big-switch routine for IP rewriting with routine of mapping-less address virtualization.
