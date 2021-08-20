import ammonite.ops._

// data

val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

case class Elimination(
  label: String,
  participants: Int,
  double: Boolean,
  rounds: Int,
  verticalSteps: Int,
  drops: Seq[Seq[Player]],
  extras: Seq[SvgContext => xml.Elem],
) {
  // symmetrical, sums to zero
  val nudges =
    (1 to (1 + rounds)).map(r => (2 + rounds - 2 * r) / 2)
}
case class Player(label: Option[String], round: Int, topOffset: Int)
case class Match(label: Option[String], p1: Player, p2: Player) {
  val winner: Player = Player(
    None,
    math.max(p1.round, p2.round) + 1,
    (p1.topOffset + p2.topOffset) / 2
  )
}

// functions

def getPower(p: Int): Int =
  if (p > 2) 1 + getPower(p / 2)
  else 1

def seedPlayers(players: Seq[Int], step: Int, steps: Int): Seq[Int] =
  if (step > steps) players
  else {
    val magic = scala.math.pow(2, step).toInt + 1
    val newPlayers = players.flatMap(p => Seq(p, magic - p))
    seedPlayers(newPlayers, step + 1, steps)
  }

def playOff(players: Seq[Player], round: Int, printLabel: Boolean): Seq[Match] = {
  players.sortBy(_.topOffset).sliding(2, 2).zipWithIndex.flatMap {
    case (List(p1, p2), i) => {
      val label =
        if (printLabel) Some(round.toString + alphabet(i))
        else None
      Some(Match(label, p1, p2))
    }
    case _ => None
  }.toSeq
}

def playerMatch(
  ps: Seq[Player],
  round: Int,
  printLabel: Boolean,
  drops: Seq[Seq[Player]],
): (Seq[Match], Seq[Seq[Player]]) = {
  val players = ps ++ drops.headOption.getOrElse(Nil)
  val matches = playOff(players, round, printLabel)
  val winners = matches.map(_.winner)

  if (winners.length <= 1 && drops.flatten.length < 1) {
    if (winners.length == 0) (matches, Seq(players))
    else (matches, Seq(winners))
  } else {
    val (matches1, winners1) =
      playerMatch(winners, round + 1, printLabel, drops.drop(1))
    (matches1 ++ matches, winners1.prepended(winners))
  }
}



// view

def loserLabel(context: SvgContext): xml.Elem =
  <text xml:space="preserve" x="180" y="1370"
      transform="rotate(-90,180,1370)" style={context.bigStyle}>
    <tspan x="0" dy="1.2em">drabinka</tspan>
    <tspan x="0" dy="1.2em"> drugiej</tspan>
    <tspan x="0" dy="1.2em"> nadziei</tspan>
  </text>

def dummyMatch(round: Int, from: Int, to: Int)(context: SvgContext): xml.Elem =
  context.getMatchPath(Match(
    None,
    Player(None, round, from),
    Player(None, round, to),
  ))

def dummyPlayer(round: Int, topOffset: Int)(context: SvgContext): xml.Elem =
  context.getPlayerPath(Player(None, round, topOffset))

case class SvgContext(height: Int, width: Int, padding: Int, e: Elimination) {
  val contentHeight = height - padding * 2
  val contentWidth = width - padding * 2
  val playerWidth = contentWidth / (e.rounds + 1)
  val fontStyle = "font-size: 40; font-family: Fira Mono; font-variant-numeric: lining-nums tabular-nums;"
  val bigStyle = "font-size: 60; font-family: Fira Mono;"
  val pathStyle = "stroke: black; stroke-width: 4; stroke-linecap: round;"

  def getPlayerCoords(p: Player): (Int, Int) = {
    val x = 40 + padding + ((p.round - 1) * contentWidth) / (e.rounds + 1)
    val y = -40 + padding + (p.topOffset * contentHeight) / e.verticalSteps
    (x, y)
  }

  def getPlayerPath(p: Player): xml.Elem = {
    val (x, y) = getPlayerCoords(p)
    val text = p.label match {
      case Some(label) => <text x={(x - 5).toString} y={y.toString}
            text-anchor="end"
            style={fontStyle}>{label}</text>
      case None => xml.Text("")
    }

    <g>
      {text}
      <path d={s"M $x,$y h $playerWidth"} style={pathStyle}/>
    </g>
  }

  def getMatchPath(m: Match): xml.Elem = {
    val (x1, y1) = getPlayerCoords(m.p1)
    val (x2, y2) = getPlayerCoords(m.p2)
    val (mx, my) = getPlayerCoords(m.winner)
    val text = m.label match {
      case Some(label) => <text x={(mx - 10).toString} y={(my + 14).toString}
            text-anchor="end" style={fontStyle}>{label}</text>
      case _ => xml.Text("")
    }
    val extend =
      if (x2 > x1) {
        <path d={s"M ${x1+playerWidth},${y1} h ${x2 - x1}"} style={pathStyle}/>
      } else xml.Text("")

    <g>
      {extend} {text}
      <path d={s"M ${x2+playerWidth},${y1} L ${x2+playerWidth},${y2}"} style={pathStyle}/>
    </g>
  }

  def getRoundPath(r: Int): xml.Elem = {
    val roundModifier =
      if (e.drops.nonEmpty && e.participants > 16) 1
      else 0

    val text = if (r == 1) "runda" else {s"${r + roundModifier}."}
    val (x, y) = getPlayerCoords(Player(None, r, e.verticalSteps))
    <text x={(x + playerWidth/2).toString} y={y.toString}
      text-anchor="middle" style={bigStyle}>{text}</text>
  }
}


def draw(e: Elimination): xml.Elem = {
  val initialPlayers: Seq[Player] = seedPlayers(Seq(1), 1, getPower(e.participants))
    .zipWithIndex.map {
      case (label, i) => Player(Some(label.toString), 1, 2+2*i)
    }

  val (players: Seq[Player], matches: Seq[Match]) =
    if (e.double) {
      if (e.drops.nonEmpty && e.participants > 16) {
        val (losermatches, losers) = playerMatch(Nil, 2, false, e.drops)
        (e.drops.flatten ++ losers.flatten,
        losermatches)
      } else {
        val (winnermatches, winners) = playerMatch(initialPlayers, 1, true, Nil)
        val (losermatches, losers) = playerMatch(Nil, 2, false, e.drops)

        val finale: Seq[Match] = playOff(winners.last ++ losers.last, e.rounds, false)
        val winner: Seq[Player] = finale.map(_.winner)

        (initialPlayers ++ winners.flatten ++ e.drops.flatten ++ losers.flatten ++ winner,
        winnermatches ++ losermatches ++ finale)
      }
    } else {
      val (winnermatches, winners) = playerMatch(initialPlayers, 1, false, Nil)
      (initialPlayers ++ winners.flatten,
      winnermatches)
    }

  val context = SvgContext(2100, 2970, 150, e)

  <svg
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      viewBox={s"0 0 ${context.width} ${context.height}"}
      width="297mm" height="210mm">
    <rect fill="white" x="0" y="0"
      width={context.width.toString} height={context.height.toString} />
    { players.map(context.getPlayerPath) }
    { matches.map(context.getMatchPath) }
    { (1 to e.rounds).map(context.getRoundPath) }
    { e.extras.map(extra => extra(context)) }
  </svg>
}

def process(e: Elimination) = {
  val pp = new xml.PrettyPrinter(100, 2)
  write.over(pwd/"drabinki-pdf"/e.label, pp.format(draw(e)))
}

val drops8 = Seq(
  Seq(
    Player(Some("1A"), 2, 20),
    Player(Some("1B"), 2, 22),
    Player(Some("1C"), 2, 26),
    Player(Some("1D"), 2, 28),
  ),
  Seq(
    Player(Some("2B"), 3, 19),
    Player(Some("2A"), 3, 25),
  ),
  Seq(),
  Seq(
    Player(Some("3A"), 5, 18),
  ),
)

val drops16 = Seq(
  Seq(
    Player(Some("1A"), 2, 34),
    Player(Some("1B"), 2, 36),
    Player(Some("1E"), 2, 39),
    Player(Some("1F"), 2, 41),
    Player(Some("1C"), 2, 44),
    Player(Some("1D"), 2, 46),
    Player(Some("1G"), 2, 49),
    Player(Some("1H"), 2, 51),
  ),
  Seq(
    Player(Some("2B"), 3, 33),
    Player(Some("2D"), 3, 38),
    Player(Some("2A"), 3, 43),
    Player(Some("2C"), 3, 48),
  ),
  Seq(
    Player(Some("3B"), 4, 32),
    Player(Some("3A"), 4, 37),
  ),
  Seq(
    Player(Some("4A"), 5, 42),
  ),
)

val drops32 = Seq(
  Seq(
    Player(Some("1A"), 1,  3),
    Player(Some("1B"), 1,  5),
    Player(Some("1C"), 1,  8),
    Player(Some("1D"), 1, 10),
    Player(Some("1I"), 1, 13),
    Player(Some("1J"), 1, 15),
    Player(Some("1K"), 1, 18),
    Player(Some("1L"), 1, 20),
    Player(Some("1E"), 1, 23),
    Player(Some("1F"), 1, 25),
    Player(Some("1G"), 1, 28),
    Player(Some("1H"), 1, 30),
    Player(Some("1M"), 1, 33),
    Player(Some("1N"), 1, 35),
    Player(Some("1O"), 1, 38),
    Player(Some("1P"), 1, 40),
  ),
  Seq(
    Player(Some("2C"), 2,  2),
    Player(Some("2D"), 2,  7),
    Player(Some("2G"), 2, 12),
    Player(Some("2H"), 2, 17),
    Player(Some("2E"), 2, 22),
    Player(Some("2F"), 2, 27),
    Player(Some("2A"), 2, 32),
    Player(Some("2B"), 2, 37),
  ),
  Seq(),
  Seq(
    Player(Some("3C"), 4,  2),
    Player(Some("3A"), 4, 12),
    Player(Some("3D"), 4, 22),
    Player(Some("3B"), 4, 32),
  ),
  Seq(
    Player(Some("4A"), 5,  8),
    Player(Some("4B"), 5, 18),
  ),
  Seq(
    Player(Some("5A"), 6, 36),
  ),
)

Seq(
  Elimination("single-elim-08.svg",  8, false, 3, 18, Nil, Nil),
  Elimination("single-elim-16.svg", 16, false, 4, 34, Nil, Nil),
  Elimination("single-elim-32.svg", 32, false, 5, 68, Nil, Nil),

  Elimination("double-elim-08.svg",  8,  true, 6,  30, drops8,  Seq(loserLabel)),
  Elimination("double-elim-16.svg", 16,  true, 7,  54, drops16, Seq(loserLabel)),

  Elimination("double-elim-32-1.svg", 32, true, 6, 68, Nil, Seq(
    dummyMatch(6, 33, 64),
    dummyPlayer(7, 60),
  )),
  Elimination("double-elim-32-2.svg", 32, true, 8, 44, drops32, Seq(
    dummyMatch(8, 21, 2),
  )),
).map(process)
